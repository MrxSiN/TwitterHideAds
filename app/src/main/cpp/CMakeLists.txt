cmake_minimum_required(VERSION 3.22.1)
project(twitterbf C)

# The Brainfuck policy core: AOT-generated programs plus the runtime and JNI
# glue. Regenerate generated/ with tools/bftool/gen.py; never edit it.
add_library(twitterbf SHARED
        bf_engine.c
        bf_jni.c
        generated/bf_programs.generated.c)

target_compile_options(twitterbf PRIVATE
        -O2
        -fvisibility=hidden
        -ffunction-sections
        -fdata-sections
        -fstack-protector-strong
        -D_FORTIFY_SOURCE=2
        -Wall
        -Werror=implicit-function-declaration)

target_link_options(twitterbf PRIVATE
        -Wl,-z,relro
        -Wl,-z,now
        -Wl,-z,noexecstack)

if (CMAKE_BUILD_TYPE STREQUAL "Release" OR CMAKE_BUILD_TYPE STREQUAL "RelWithDebInfo")
    target_compile_options(twitterbf PRIVATE -flto)
    target_link_options(twitterbf PRIVATE -flto -Wl,--gc-sections -Wl,--icf=all)
endif ()
