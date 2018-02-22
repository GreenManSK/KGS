#include <stdint.h>
#define __int64 int64_t

#include "cz_muni_fi_kurcik_kgs_util_Majka.h"
#include	"majka.h"
#include	<iostream>
#include	<string.h>

/*
 * Class:     cz_muni_fi_kurcik_kgs_util_Majka
 * Method:    find
 * Signature: (Ljava/lang/String;Ljava/lang/String;I)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_cz_muni_fi_kurcik_kgs_util_Majka_find
	(JNIEnv *env, jobject object, jstring dic, jstring word, jint flags) {
	const char *nativeDic = env->GetStringUTFChars(dic, 0);
	const char *nativeWord = env->GetStringUTFChars(word, 0);

	fsa majka(nativeDic);

	char * results = new char[majka.max_results_size];
	int rc = majka.find(nativeWord, results, (int) flags);

	jobjectArray ret = (jobjectArray)env->NewObjectArray(rc,env->FindClass("java/lang/String"),env->NewStringUTF(""));
	const char * result;
	int i;
	for (result = results, i = 0; i < rc; i++, result += strlen(result) + 1) {
		env->SetObjectArrayElement(ret,i,env->NewStringUTF(result));
	}

	return ret;
}

/*
 * Class:     cz_muni_fi_kurcik_kgs_util_Majka
 * Method:    findAll
 * Signature: (Ljava/lang/String;[Ljava/lang/String;II)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_cz_muni_fi_kurcik_kgs_util_Majka_findAll
	(JNIEnv *env, jobject object, jstring dic, jobjectArray words, jint wordCount, jint flags) {
	const char *nativeDic = env->GetStringUTFChars(dic, 0);

	fsa majka(nativeDic);

	jobjectArray rows = (jobjectArray)env->NewObjectArray(wordCount,env->FindClass("java/lang/String"),env->NewStringUTF(""));
	int i, rc;
	char * results = new char[majka.max_results_size];
	for (i = 0; i < wordCount; i++) {
		jobject word = env->GetObjectArrayElement(words, i);
		const char *nativeWord = env->GetStringUTFChars((jstring) word, 0);
		rc = majka.find(nativeWord, results, (int) flags);

		if (rc <= 0) {
			env->SetObjectArrayElement(rows,i,env->NewStringUTF(nativeWord));
		} else {
			env->SetObjectArrayElement(rows,i,env->NewStringUTF(results));
		}
	}

	return rows;

}