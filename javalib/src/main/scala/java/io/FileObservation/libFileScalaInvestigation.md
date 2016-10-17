#About

this document is kind of a personal journal of my investigation of the native code called by the library File.java, so I don't forget what each method does and what each macros means. 

## native existsImpl
```
JNIEXPORT jboolean JNICALL
Java_java_io_File_existsImpl (JNIEnv * env, jobject recv, jbyteArray path)
{
  PORT_ACCESS_FROM_ENV (env);
  I_32 result;
  char pathCopy[HyMaxPath];
  jsize length = (*env)->GetArrayLength (env, path);
  if (length > HyMaxPath-1) {
    throwPathTooLongIOException(env, length);
    return 0;
  }
  ((*env)->GetByteArrayRegion (env, path, 0, length, (jbyte *)pathCopy));
  pathCopy[length] = '\0';
  result = hyfile_attr (pathCopy);
  return result >= 0;
}
```

### hyfile_attr(pathCopy)
```
/**
 * Determine whether path is a file or directory.
 *
 * @param[in] portLibrary The port library
 * @param[in] path file/path name being queried.
 *
 * @return HyIsFile if a file, HyIsDir if a directory, negative portable error code on failure.
 */
I_32 VMCALL
hyfile_attr (struct HyPortLibrary *portLibrary, const char *path)
{
  struct stat buffer;

  /* Neutrino does not handle NULL for stat */

  if (stat (path, &buffer))
    {
      return portLibrary->error_set_last_error (portLibrary, errno,
                                                findError (errno));
    }
  if (S_ISDIR (buffer.st_mode))
    {
      return HyIsDir;
    }
  return HyIsFile;
}
```
#### struct stat buffer;
This as well as the function stat  can be accessed on the clibrary ```sys/types.h 
sys/stat.h 
unistd.h```



## native deleteDirImpl

```
JNIEXPORT jboolean JNICALL
Java_java_io_File_deleteDirImpl (JNIEnv * env, jobject recv, jbyteArray path)
{
  PORT_ACCESS_FROM_ENV (env);
  I_32 result;
  jsize length = (*env)->GetArrayLength (env, path);
  char pathCopy[HyMaxPath];
  if (length > HyMaxPath-1) {
    throwPathTooLongIOException(env, length);
    return 0;
  }
  ((*env)->GetByteArrayRegion (env, path, 0, length, (jbyte *)pathCopy));
  pathCopy[length] = '\0';
  result = hyfile_unlinkdir (pathCopy);
  return result == 0;
}
```

### hyfile_unlinkdir(const car * path)
* found in classlib/modules/portlib/src/main/native/include/shared/hyport.h though as a macro
* but also in classlib/modules/portlib/src/main/native/port/unix/hyfile.c`
* and windows version in /classlib/modules/portlib/src/main/native/port/windows/hyfile.c , by the way this one is pure hell : 
```
I_32 VMCALL
hyfile_unlinkdir (struct HyPortLibrary * portLibrary, const char *path)
{
  wchar_t *pathW;
  convert_path_to_unicode(portLibrary, path, &pathW);

  /* should be able to delete read-only dirs, so we set the file attribute back to normal */
  if (0 == SetFileAttributesW (pathW, FILE_ATTRIBUTE_NORMAL))
    {
      I_32 error = GetLastError ();
      portLibrary->error_set_last_error (portLibrary, error, findError (error));	/* continue */
    }

  if (RemoveDirectoryW (pathW))
    {
      portLibrary->mem_free_memory(portLibrary, pathW);
      return 0;
    }
  else
    {
      I_32 error = GetLastError ();
      portLibrary->mem_free_memory(portLibrary, pathW);
      portLibrary->error_set_last_error (portLibrary, error,
					 findError (error));
      return -1;
    }
}
```

### getByteArrayRegion(JNIEnv *env, ArrayType array, jsize start, jsize len, NativeType (ptr) buf)
copies a region of a primitive array into a buffer



## native byte rootsImpl:
```
JNIEXPORT jobject JNICALL
Java_java_io_File_rootsImpl (JNIEnv * env, jclass clazz)
{
  char rootStrings[HyMaxPath], *rootCopy;
  I_32 numRoots;
  I_32 index = 0;
  jarray answer;

/**
 * It is the responsibility of #getPlatformRoots to return a char array
 * with volume names separated by null with a trailing extra null, so for
 * Unix it should be '\<null><null>' .
 */
  numRoots = getPlatformRoots (rootStrings);
  if (numRoots == 0)
    return NULL;
  rootCopy = rootStrings;

  answer =
    (*env)->NewObjectArray (env, numRoots, HARMONY_CACHE_GET (env, CLS_array_of_byte), NULL);
  if (!answer)
    {
      return NULL;
    }
  while (TRUE)
    {
      jbyteArray rootname;
      jsize entrylen = strlen (rootCopy);
      /* Have we hit the second null? */
      if (entrylen == 0)
        break;
      rootname = (*env)->NewByteArray (env, entrylen);
      (*env)->SetByteArrayRegion (env, rootname, 0, entrylen, (jbyte *) rootCopy);
      (*env)->SetObjectArrayElement (env, answer, index++, rootname);
      (*env)->DeleteLocalRef (env, rootname);
      rootCopy = rootCopy + entrylen + 1;   //increase of the pointer size
    }
  return answer;
}
```
**HyMaxPath** = 1024 was chosen from unix MAXPATHLEN.  Override in platform specific hyfile implementations if needed.

#### NewObjectArray(JNIEnv * env, jsize lengths, jclass elementClass, jobject initialElement) 
* "Constructs a new array holding objects in class _elementClass_. All elements are initially set to _initialElement_."
* found in harmony/drlvm/vm/vmcore/src/jni/jni_array.cpp
(
* In this utilisation, at the initialisation, the answer is an array of Length 1, whose class is an array of byte, the only element of the array is set to NULL.

#### NewByteArray(JNIEnv *, jsize length)
* A family of operations used to construct a new primitive array object. Table 4-8 describes the specific primitive array constructors. You should replace New"PrimitiveType"Array with one of the actual primitive array constructor routine names from the following table, and replace ArrayType with the corresponding array type for that routine.

* Everythin in the title, the primitives are java's primitives

#### SetByteArrayRegions (JNIEnv* env, ArrayType array, jsize start, jsize len, const NativeType (ptr) buf);
* "A family of functions that copies back a region of a primitive array from a buffer."
* In this utilisation, it sets rootname (which is a java array of byte) to the values found from start (0) to the len (entrylen) of rootCopy
* In fact is simply doing copying to the array of byte from java.

#### DeleteLocalRef(JNIEnv * env, jobject localRef)
* Found in drlvm/vm/vmcore/src/jni/jni.cppp
* "Deletes the local reference pointed to by _localRef_.
* Seems to be useless, only call oh_discard_local_handle, which seems to make its argument Null (before that disabling recursive and after that renabling it)

#### SetObjectArrayElement(JNIEnv * jni_env, jobjectArray array, jsize index, jobject value)
Found in drlvm/vm/vmcore/src/jni/jni.cppp
Seems to be the one setting something here.

#### getPlatformRoot(const char * str)
**getPlatformRoot(char* rootStrings)**: function in classlib/modules/luni/src/main/native/luni/unix/helpers.c
```
I_32
getPlatformRoots (char *rootStrings)
{
  rootStrings[0] = (char) '/';
  rootStrings[1] = (char) 0;
  rootStrings[2] = (char) 0;
  return 1;
}
```
##Conclusion
A lot of javaism down here, the only thing really useful is the stub rootCopy = rootCopy + entrylen + 1, it will increase the pointer to where should the other roots be, and then copy this few bytes to the array of array of bytes.



## native oneTimeInitialisation:

```
JNIEXPORT void JNICALL
Java_java_io_File_oneTimeInitialization (JNIEnv * env, jclass clazz)
{
  jclass arrayClass = (*env)->FindClass (env, "[B");
  if (arrayClass)
    {
      jobject globalRef = (*env)->NewGlobalRef (env, arrayClass);
      if (globalRef)
        HARMONY_CACHE_SET (env, CLS_array_of_byte, globalRef);
    }
  return;
}
```

#### FindClass
**FindClass(JNIEnv * env, const char * name)** java wrapper to find class. Useless.

#### NewGlobalRef
java wrapper to get the reference of the object. Useless again

#### HARMONY_CACHE_SET
**HARMONY_CACHE_SET(JNIEnv * env, jclass x, jobject v)** : macro defined in classlib/modules/portlib/src/main/native/include/shared/libglob.h
	_: (((JniIDCache*) HY_VMLS_GET((env), HARMONY_ID_CACHE))->x = (v))_
	**HY_VMLS_GET(env, key)** : (HY_VMLS_FNTBL(env)->HyVMLSGet(env, (key)))
		**HY_VLMS_FNTBL(env):** ...

### Conclusion
this function is useless as such for scala native, since it is called only one time on the static bloc and all it do is javaism, can be dropped.
		
