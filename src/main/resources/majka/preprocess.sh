#!/bin/bash

#preprocess input_file output_file

# Get words form file in first argument
words=$(cat $1 | grep -o -E "\w+")

#Use majka on all words
majkovane=$(for X in $words ; do 
	a=$(echo $X | ./majka -f majka.w-lt | head -n1 | sed 's/:.*$//');
	[ -z "$a" ] && echo $X || echo $a; #Majka return nothing if word is not in its database
done);

#Remove stop words and saves them into file in second argument
echo " $majkovane " | sed -f stop_words.sed | tr '\n' ' ' | sed -e "s/  */ /g" > $2