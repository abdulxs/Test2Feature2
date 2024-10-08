#!/bin/bash

# #repository URL
REPGIT="https://github.com/abdulxs/Experiment"
#Name of System Folder
SYSFOLDER="experiment"
#Name of Test Folder
TESTFOLDER="test"
#Commit Sha
NCOMMIT="6a1843a1009df0f3e37269a42d0961c065d98c90"

#PATH System
PATHSYS="${PWD}/database/${SYSFOLDER}/"
#PATH Tests
PATHTEST="${PWD}/database/${SYSFOLDER}/${TESTFOLDER}"
#PATH
PATHSCRIPT=$PWD
#PATH XML Files
PATHXML="${PATHSYS}xml"

if [ -d database/ ];
then
cd database
if [ -d clean ];
then
rm -r clean
fi
if [ -d $SYSFOLDER ];
then
echo "System already exists"
else
git clone $REPGIT
fi
else
echo "Creating the folder"
mkdir database
cd database
git clone $REPGIT
fi


# Exec Feature Location
echo "Running MineFeaturesLines"
cd $PATHSCRIPT
cd MineFeaturesLines
gradle run -Pmyargs=$PATHSYS,"${PATHSCRIPT}/result-featurelocation",$NCOMMIT

#Exec Doxygen
echo "Running Doxygen"
cd $PATHSCRIPT
cp -a Doxygen/Doxyfile $PATHSYS
cd $PATHSYS
doxygen Doxyfile

#Exec ParserXML
echo "Running MineTestLines"
cd "${PATHSCRIPT}/MineTestLines"
python3 MineTestLines.py $PATHTEST $PATHXML $TESTFOLDER

# Exec Merge Feature X Tests
echo "Running MergeTestFeaturesLines"
cd "${PATHSCRIPT}/MergeTestFeaturesLines"
python3 MergeTestFeaturesLines.py "${PATHSCRIPT}/MineTestLines/result/MineTestLines.csv" "${PATHSCRIPT}/result-featurelocation/${NCOMMIT}/FilesFeature/feature.csv"