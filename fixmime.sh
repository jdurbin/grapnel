#!/bin/sh 
find . -name \*.html -exec svn propset svn:mime-type text/html {} \;