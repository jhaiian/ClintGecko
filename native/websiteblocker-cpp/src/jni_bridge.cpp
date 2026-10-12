#include <jni.h>
#include <cstdio>
#include <string>

#include "website_blocker.h"

using websiteblocker::Builder;
using websiteblocker::Database;

namespace {

std::string JStringToStd(JNIEnv* env, jstring value) {
    if (!value) return std::string();
    const char* chars = env->GetStringUTFChars(value, nullptr);
    std::string result(chars ? chars : "");
    if (chars) env->ReleaseStringUTFChars(value, chars);
    return result;
}

jstring StdToJString(JNIEnv* env, const std::string& value) {
    return env->NewStringUTF(value.c_str());
}

}  // namespace

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_jhaiian_clint_blocker_engine_WebsiteBlockerNative_nativeCreateBuilder(JNIEnv*, jobject) {
    return reinterpret_cast<jlong>(new Builder());
}

JNIEXPORT jstring JNICALL
Java_com_jhaiian_clint_blocker_engine_WebsiteBlockerNative_nativeAddText(
    JNIEnv* env, jobject, jlong builderPtr, jstring text) {
    auto* builder = reinterpret_cast<Builder*>(builderPtr);
    if (!builder) return StdToJString(env, "{\"added\":0,\"skipped\":0}");

    std::string content = JStringToStd(env, text);
    Builder::Stats stats = builder->AddText(content);

    char buffer[128];
    std::snprintf(buffer, sizeof(buffer), "{\"added\":%llu,\"skipped\":%llu}",
                  static_cast<unsigned long long>(stats.added),
                  static_cast<unsigned long long>(stats.skipped));
    return StdToJString(env, buffer);
}

JNIEXPORT jstring JNICALL
Java_com_jhaiian_clint_blocker_engine_WebsiteBlockerNative_nativeFinalizeBuilder(
    JNIEnv* env, jobject, jlong builderPtr, jstring outputPath) {
    auto* builder = reinterpret_cast<Builder*>(builderPtr);
    std::string path = JStringToStd(env, outputPath);

    if (!builder) return StdToJString(env, "{\"success\":false,\"domainCount\":0,\"sizeBytes\":0}");

    uint64_t domainCount = 0;
    uint64_t sizeBytes = 0;
    bool success = builder->Finalize(path, &domainCount, &sizeBytes);

    char buffer[160];
    std::snprintf(buffer, sizeof(buffer), "{\"success\":%s,\"domainCount\":%llu,\"sizeBytes\":%llu}",
                  success ? "true" : "false",
                  static_cast<unsigned long long>(domainCount),
                  static_cast<unsigned long long>(sizeBytes));
    return StdToJString(env, buffer);
}

JNIEXPORT void JNICALL
Java_com_jhaiian_clint_blocker_engine_WebsiteBlockerNative_nativeDestroyBuilder(
    JNIEnv*, jobject, jlong builderPtr) {
    delete reinterpret_cast<Builder*>(builderPtr);
}

JNIEXPORT jlong JNICALL
Java_com_jhaiian_clint_blocker_engine_WebsiteBlockerNative_nativeLoadDatabase(
    JNIEnv* env, jobject, jstring path) {
    std::string p = JStringToStd(env, path);
    Database* db = Database::Load(p);
    return reinterpret_cast<jlong>(db);
}

JNIEXPORT jboolean JNICALL
Java_com_jhaiian_clint_blocker_engine_WebsiteBlockerNative_nativeIsBlocked(
    JNIEnv* env, jobject, jlong handle, jstring host) {
    auto* db = reinterpret_cast<Database*>(handle);
    if (!db) return JNI_FALSE;
    std::string h = JStringToStd(env, host);
    return db->IsBlocked(h) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jlong JNICALL
Java_com_jhaiian_clint_blocker_engine_WebsiteBlockerNative_nativeDomainCount(
    JNIEnv*, jobject, jlong handle) {
    auto* db = reinterpret_cast<Database*>(handle);
    return db ? static_cast<jlong>(db->DomainCount()) : 0L;
}

JNIEXPORT void JNICALL
Java_com_jhaiian_clint_blocker_engine_WebsiteBlockerNative_nativeUnloadDatabase(
    JNIEnv*, jobject, jlong handle) {
    delete reinterpret_cast<Database*>(handle);
}

}  // extern "C"
