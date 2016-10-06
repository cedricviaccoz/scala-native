#About

this document is kind of a personal journal of my investigation of the native code called by the library File.java, so I don't forget what each method does and what each macros means. 

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

### HARMONY_CACHE_SET
**HARMONY_CACHE_SET(JNIEnv * env, jclass x, jobject v)** : macro defined in classlib/modules/portlib/src/main/native/include/shared/libglob.h
	_: (((JniIDCache*) HY_VMLS_GET((env), HARMONY_ID_CACHE))->x = (v))_
	HY_VMLS_GET: 
