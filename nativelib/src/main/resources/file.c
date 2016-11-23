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
#include <utime.h>
#include <dirent.h>
#include <string.h>
#include <stdlib.h>
#include <errno.h>

//values chosen accordingly to the corresponding "Hy" macros in Apache Harmony
#define isDir 0 
#define isFile 1
#define newFilImplFlag (O_TRUNC | O_CREAT | O_EXCL | O_RDWR)
#define newFileOrigFlags 0x8A
#define newFileImplMode 0666


#ifdef ZOS
#define FD_BIAS 1000
#undef fwrite
#undef fread
#else
#define FD_BIAS 0
#endif /* ZOS */


//#define TEST

typedef void * UDATA;

UDATA scalanative_file_findfirst(char * path, char* resultbuf){

#if defined(_AIX)
  DIR64 *dirp = NULL;
  struct dirent64 *entry;
#else
  DIR *dirp = NULL;
  struct dirent *entry;
#endif

#if defined(_AIX)
  dirp = opendir64 (path);
#else
  dirp = opendir (path);
#endif


  if (dirp == NULL)
    {
      return (UDATA) - 1;
    }
#if defined(_AIX)
  entry = readdir64 (dirp);
#else
  entry = readdir (dirp);
#endif


  if (entry == NULL)
    {
#if defined(_AIX)
      closedir64 (dirp);
#else
      closedir (dirp);
#endif
      return (UDATA) - 1;
    }
  strcpy (resultbuf, entry->d_name);
  return dirp;
}


int32_t scalanative_file_findnext (UDATA findhandle, char *resultbuf)
{
#if defined(_AIX)
  struct dirent64 *entry;
#else
  struct dirent *entry;
#endif


#if defined(_AIX)
  entry = readdir64 ((DIR64 *) findhandle);
#else
  entry = readdir ((DIR *) findhandle);
#endif


  if (entry == NULL)
    {
      return -1;
    }
  strcpy (resultbuf, entry->d_name);
  return 0;
}


void file_findclose (UDATA findhandle)
{
#if defined(_AIX)
  closedir64 ((DIR64 *) findhandle);
#else
  closedir ((DIR *) findhandle);
#endif
}

int scalanative_set_last_mod(char *path, int64_t time)
{
  struct stat statbuf;
  struct utimbuf timebuf;
  if (stat(path, &statbuf)){
      return 0;  
  }
  timebuf.actime = statbuf.st_atime;
  timebuf.modtime = (time_t) (time / 1000);
  return utime (path, &timebuf) == 0;

}

int scalanative_set_read_only_native(char * path)
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

int scalanative_file_mkdir(const char * path){
  if (-1 == mkdir (path, S_IRWXU | S_IRWXG | S_IRWXO))
    {
      return -1;
    }
    return 0;
}

int64_t scalanative_file_length(const char * path){
  struct stat st;

  if(stat(path, &st)){
    return -1;
  }
  return (int64_t) st.st_size;
}

uint64_t scalanative_last_mod(const char * path){
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
int scalanative_file_open(const char * path, const int mode){
  struct stat buffer;
  int32_t fd;
  int32_t fdflags;
  stat(path, &buffer);
  fd = open(path, newFilImplFlag, newFileImplMode);
  if(fd == -1){
    switch(errno){
      //the file already exists
      case EEXIST: return -2;
      default: return -1;
    }
  }
  fdflags = fcntl(fd, F_GETFD, 0);
  fcntl(fd, F_SETFD, fdflags | FD_CLOEXEC);
  fd += FD_BIAS;
  return (int) fd;
}

int scalanative_file_descriptor_close(int fd){

#if (FD_BIAS != 0)
    if (fd < FD_BIAS) {
        /* Cannot close STD streams, and no other FD's should exist <FD_BIAS*/
      return -1;
    }
#endif

    return close ((int) (fd - FD_BIAS));

}

char scalanative_separator_char(){
#ifdef _WIN32
  return '\\';
#else
  return '/';
#endif
}

char scalanative_path_separator_char(){
#ifdef _WIN32
  return ';';
#else
  return ':';
#endif
}

int scalanative_is_case_sensitive(){
#ifdef _WIN32
  return 0;
#else
  return 1;
#endif
}

//works only for unix system.
int scalanative_get_platform_roots (char *rootStrings){
  rootStrings[0] = (char) '/';
  rootStrings[1] = (char) 0;
  rootStrings[2] = (char) 0;
  return 1;
}

//ported from hyfile_attr function in classlib/modules/portlib/src/main/native/port/unix/hyfile.c
int scalanative_file_attr(const char *path)
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

const char * scalanative_get_os_encoding(){
  setlocale(LC_ALL, "");
  return nl_langinfo(CODESET);
}

const char * scalanative_get_temp_dir(){
  char const * folder = getenv("TMPDIR");
  if (folder == 0) folder = "/tmp";
  return folder;
}


#ifdef TEST
int main(void){
  printf("userdir %s\n", scalanative_get_user_dir());
  //printf("tmp folder: %s", scalanative_get_temp_dir());
  return 0;
}
#endif