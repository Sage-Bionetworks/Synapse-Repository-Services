import os, sys, fileinput
'''
Walk the complete PLFM hierarchy and bring all the pom.xml files up to new minor version
'''

#Old and new minor versions
oldVersion = '0.13.2-SNAPSHOT'
newVersion = '0.13.3'
#Path to PLFM on your system
startPath = os.getcwd()
count = 0;
for root, subFolders, files in os.walk(startPath):    
    for file in files:
        if file == 'pom.xml': 
            f = os.path.join(root, file) 
            print 'checking '+f
            for line in fileinput.FileInput(f, inplace=1):
                found = False
                if line.find('<version>'+oldVersion+'</version>') >= 0 and not found:
                    line = line.replace(oldVersion,newVersion)
                    found = True
                    count += 1
                print line.rstrip()
print 'updated: '+str(count)+' pom.xml files'