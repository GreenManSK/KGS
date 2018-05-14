cmake_minimum_required (VERSION 2.6)
project (kgs)

find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS})
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fPIC -g -O2 -std=gnu++0x --pedantic -Wall -Wextra -DUTF")

# Based on https://stackoverflow.com/posts/32259072/revisions
if(UNIX AND NOT APPLE)
	set(LINUX TRUE)
endif()

add_library(majkaj SHARED cz_muni_fi_kurcik_kgs_util_Majka.cpp)

if(LINUX) 
	add_custom_command(OUTPUT ${CMAKE_CURRENT_SOURCE_DIR}/linux/majka.o
			COMMAND make majka.o
			WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/linux
			)
	add_custom_target(majka_lib DEPENDS ${CMAKE_CURRENT_SOURCE_DIR}/linux/majka.o)
	target_link_libraries(majkaj ${CMAKE_CURRENT_SOURCE_DIR}/linux/majka.o)
else()
	add_custom_command(OUTPUT ${CMAKE_CURRENT_SOURCE_DIR}/windows/majka.o
			COMMAND make
			WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/windows
			)
	add_custom_target(majka_lib DEPENDS ${CMAKE_CURRENT_SOURCE_DIR}/windows/majka.o)
	target_link_libraries(majkaj ${CMAKE_CURRENT_SOURCE_DIR}/windows/majka.o)
endif()

add_dependencies(majkaj majka_lib)