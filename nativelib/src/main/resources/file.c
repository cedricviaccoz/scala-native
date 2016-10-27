#include <stdio.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h> // for Flags O_*
#include <locale.h>
#include <langinfo.h>
#include <limits.h>
#include <fcntl.h> // for open
#include <stdint.h> // for int32_t
#include <time.h> // for tzet()

//values chosen accordingly to the corresponding HyIsFile and HyIsDir in harmony
#define isDir 0 
#define isFile 1


#define newFilImplFlag (O_TRUNC | O_CREAT | O_EXCL | O_RDWR)
#define newFileImplMode 0666

#ifdef ZOS
#define FD_BIAS 1000
#undef fwrite
#undef fread
#else
#define FD_BIAS 0
#endif /* ZOS */



//#define TEST


int setReadOnlyNative(char * path)
{
  struct stat buffer;
  mode_t mode;
  if (stat (path, &buffer))
    {
      return 0;
    }
  mode = buffer.st_mode;
  mode = mode & 07555;
  return chmod (path, mode) == 0;
}

int file_mkdir(const char * path){
  if (-1 == mkdir (path, S_IRWXU | S_IRWXG | S_IRWXO))
    {
      return -1;
    }
    return 0;
}

int64_t file_length(const char * path){
  struct stat st;

  if(stat(path, &st)){
    return -1;
  }
  return (int64_t) st.st_size;
}

uint64_t lastmod(const char * path){
  struct stat st;
  tzset();


  if (stat (path, &st))
    {
      return -1;
    }
  return (uint64_t)st.st_mtime * 1000;
}


/*this is a reimplementation of hyfile_open,
  with all of its parameter and branches simplified
  in order to only create à new File.*/
int new_file_impl(const char * path){
  int32_t fd;
  int32_t fdflags;
  fd = open(path, newFilImplFlag, newFileImplMode);
  if(fd == -1){
    return -1;
  }
  fdflags = fcntl(fd, F_GETFD, 0);
  fcntl(fd, F_SETFD, fdflags | FD_CLOEXEC);
  fd += FD_BIAS;
  return (int) fd;
}

int fileDescriptor_close(int fd){

#if (FD_BIAS != 0)
    if (fd < FD_BIAS) {
        /* Cannot close STD streams, and no other FD's should exist <FD_BIAS */
      return -1;
    }
#endif

    return close ((int) (fd - FD_BIAS));

}

char separatorChar(){
#ifdef _WIN32
  return '\\';
#else
  return '/';
#endif
}

char pathSeparatorChar(){
#ifdef _WIN32
  return ';';
#else
  return ':';
#endif
}

int isCaseSensitiveImpl(){
#ifdef _WIN32
  return 0;
#else
  return 1;
#endif
}

//works only for unix system.
int getPlatformRoots (char *rootStrings){
  rootStrings[0] = (char) '/';
  rootStrings[1] = (char) 0;
  rootStrings[2] = (char) 0;
  return 1;
}

//ported from hyfile_attr function in classlib/modules/portlib/src/main/native/port/unix/hyfile.c
int file_attr(const char *path)
{
  struct stat buffer;

  if (stat(path, &buffer))
    {
      return -1;
    }
  if (S_ISDIR (buffer.st_mode))
    {
      return isDir;
    }
  return isFile;
}

const char * getOsEncoding(){
  setlocale(LC_ALL, "");
  return nl_langinfo(CODESET);
}

const char * getUserDir(){
    char buff[PATH_MAX] = "";
    char * res = getcwd(buff, PATH_MAX-1);
    if(res != NULL){
      return res;
    }else{
      fprintf(stderr, "getcwd() error");
    }
}



#ifdef TEST
int main(void){
  printf("OS encoding is %s\n", getOsEncoding());
  printf("UserDir is %s\n", getUserDir());
  printf("Current time is %ld", (long) apr_time_now());
  return 0;
}
#endif