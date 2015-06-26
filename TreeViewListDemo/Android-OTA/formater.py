'''
Created on 21-03-2011

@author: maciek
'''


def formatString(format, **kwargs):
    '''
    '''
    if not format:  return ''
    
    for arg in kwargs.keys():
        format = format.replace("{" + arg + "}", "##" + arg + "##")
    format = format.replace ("{", "{{")
    format = format.replace("}", "}}")
    for arg in kwargs.keys():
        format = format.replace("##" + arg + "##", "{" + arg + "}")
    
    res = format.format(**kwargs)
    res = res.replace("{{", "{")
    res = res.replace("}}", "}")
    return res