#About

this document is kind of a personal journal of my investigation of the native code called by the library File.java, so I don't forget what each method does and what each macros means. 

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
    (*env)->NewObjectArray (env, numRoots,
                            HARMONY_CACHE_GET (env, CLS_array_of_byte), NULL);
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
      (*env)->SetByteArrayRegion (env, rootname, 0, entrylen,
                                  (jbyte *) rootCopy);
      (*env)->SetObjectArrayElement (env, answer, index++, rootname);
      (*env)->DeleteLocalRef (env, rootname);
      rootCopy = rootCopy + entrylen + 1;
    }
  return answer;
}
```

**HyMaxPath** = 1024 was chosen from unix MAXPATHLEN.  Override in platform 
		specific hyfile implementations if needed.

### NewObjectArray(JNIEnv * env, I_32 numRoots, 
	**HARMONY_CACHE_GET(env, x)** : macro defined in classlib/modules/portlib/src/main/native/include/shared/libglob.h
		(((JniIDCache*) HY_VMLS_GET((env), HARMONY_ID_CACHE))->x)
		the JniIDCache is a macro for **LUNIJniDCache** which can be found at classlib/modules/luni/src/main/native/luni/shared/harmonyglob.h the x in this function context refers to the field CLS_array_byte of this struct. 

### getPlatformRoot(const char * str)
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

### FindClass
**FindClass(JNIEnv * env, const char * name)** java wrapper to find class. Useless.

###NewGlobalRef
java wrapper to get the reference of the object. Useless again

### HARMONY_CACHE_SET
**HARMONY_CACHE_SET(JNIEnv * env, jclass x, jobject v)** : macro defined in classlib/modules/portlib/src/main/native/include/shared/libglob.h
	_: (((JniIDCache*) HY_VMLS_GET((env), HARMONY_ID_CACHE))->x = (v))_
	**HY_VMLS_GET(env, key)** : (HY_VMLS_FNTBL(env)->HyVMLSGet(env, (key)))
		**HY_VLMS_FNTBL(env):** ...

## Conclusion
this function is useless as such for scala native, since it is called only one time on the static bloc and all it do is javaism, can be dropped.
		
