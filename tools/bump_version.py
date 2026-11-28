# -*- coding: utf-8 -*-
"""Raise the app's version by one: versionCode + 1 and the last part of versionName + 1
(0.13.0 -> 0.13.1). Prints the new versionName. Used by the suggestions import job."""
import os
import re

PATH = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'app', 'build.gradle.kts')

with open(PATH, encoding='utf-8', newline='') as f:
    s = f.read()
code = int(re.search(r'versionCode = (\d+)', s).group(1))
name = re.search(r'versionName = "([^"]+)"', s).group(1)
parts = name.split('.')
parts[-1] = str(int(parts[-1]) + 1)
new_name = '.'.join(parts)
s = s.replace('versionCode = %d' % code, 'versionCode = %d' % (code + 1), 1)
s = s.replace('versionName = "%s"' % name, 'versionName = "%s"' % new_name, 1)
with open(PATH, 'w', encoding='utf-8', newline='') as f:
    f.write(s)
print(new_name)
