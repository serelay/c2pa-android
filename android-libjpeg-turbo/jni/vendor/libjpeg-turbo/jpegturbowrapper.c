// Copyright © 2021 Serelay Ltd. All rights reserved.
#include <turbojpeg.h>
#include <jni.h>
#include <android/log.h>
#include <syslog.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>

JNIEXPORT jbyteArray
Java_com_serelay_c2pa_ThumbnailHelper_getThumbnail(JNIEnv *env,
		jobject this,
		jbyteArray data,
		jint dataLength,
		jint width,
		jint height) {
	long unsigned int jpegSize = dataLength;
	unsigned char *compressedImage;
	jboolean isCopy;
	compressedImage = (unsigned char *)(*env)->GetByteArrayElements(env, data, &isCopy);
	int flags = 0; // any flags throws an error

	int h = height, w = width;

	// Then resize the image
    unsigned char *imgBuf = NULL;
	int pixelFormat = TJPF_BGRX;

	int len = w * h * tjPixelSize[pixelFormat];
    if ((imgBuf = (unsigned char *)tjAlloc(len)) == NULL) {
        syslog(LOG_CRIT,"Error allocating for downscale\n");
        return NULL;
    }

    tjhandle tjInstance = tjInitDecompress();
    if (tjDecompress2(tjInstance, compressedImage, dataLength, imgBuf, w, 0, h, pixelFormat, flags) < 0){
        syslog(LOG_CRIT,"Error decompressing JPEG image for downscale. %s\n", tjGetErrorStr2(tjInstance));
        return NULL;
    }

    // Then reconvert to JPEG
    tjInstance = tjInitCompress();
    unsigned char *jpegBuf = NULL;
    if (tjCompress2(tjInstance, imgBuf, w, 0, h, pixelFormat,
                    &jpegBuf, &jpegSize, TJSAMP_444, 80, flags) < 0) {
        syslog(LOG_CRIT,"Error compressing JPEG image after downscale: %s\n", tjGetErrorStr2(tjInstance));
        return NULL;
    }

    // Free up the rotated image data
    tjFree(imgBuf);

    jbyteArray array = (*env)->NewByteArray(env, jpegSize);
    (*env)->SetByteArrayRegion (env, array, 0, jpegSize, (jbyte*)(jpegBuf));
    // Free up the JPEG buffer
    tjFree(jpegBuf);
    return array;
}
