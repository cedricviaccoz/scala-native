#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h>

//values chosen accordingly to the corresponding HyIsFile and HyIsDir in harmony
#define isDir 0 
#define isFile 1

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