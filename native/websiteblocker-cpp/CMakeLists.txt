cmake_minimum_required(VERSION 3.22)
project(websiteblocker LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

if(NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Release)
endif()

set(CMAKE_INTERPROCEDURAL_OPTIMIZATION ON)
set(CMAKE_CXX_FLAGS_RELEASE "-O3 -fvisibility=hidden -fno-exceptions -fno-rtti")

add_library(websiteblocker SHARED
    src/website_blocker.cpp
    src/jni_bridge.cpp
)

target_include_directories(websiteblocker PRIVATE src)

find_library(log-lib log)
target_link_libraries(websiteblocker ${log-lib})

set_target_properties(websiteblocker PROPERTIES
    LINK_FLAGS "-Wl,--exclude-libs,ALL -Wl,--gc-sections"
)
