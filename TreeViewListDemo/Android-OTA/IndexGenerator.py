'''
Created on 21-03-2011

@author: maciek
'''
from formater import formatString
import os


class IndexGenerator(object):
    '''
    Generates Index.html for iOS app OTA distribution
    '''
    basePath = os.path.dirname(__file__)
    templateFile = os.path.join(basePath,"templates/index.tmpl")
    releaseUrls = ""
    appName = ""
    changeLog = ""
    description = ""
    version = ""
    release = ""

    def __init__(self,appName, releaseUrls, changeLog, description, version, releases):
        '''
        Constructor
        '''
        self.appName = appName
        self.releaseUrls = releaseUrls
        self.changeLog = changeLog
        self.description = description
        self.version = version
        self.releases = releases

    def get(self):
        '''
        returns index.html source code from template file
        '''

        urlList = self.releaseUrls.split(",")
        releaseList = self.releases.split(",")

        generatedHtml=""

        count=0;
        for release in releaseList:
            generatedHtml += "              <li>\n"
            generatedHtml += "                 <h3><a href=\"javascript:load('" + urlList[count] + "')\">" + release + "</a></h3>\n"
            generatedHtml += "              </li>\n"
            count += 1

        template = open(self.templateFile).read()
        index = formatString(template, downloads=generatedHtml,
                                changeLog=self.changeLog,
                                appName=self.appName,
                                description=self.description,
                                version = self.version);
        return index