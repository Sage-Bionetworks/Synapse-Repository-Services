#
# Duplicates pam_permit.c
#
# return pamh.PAM_SUCCESS
# return pamh.PAM_AUTH_ERR
import pycurl
import simplejson as json
import sys
import syslog
import re
from StringIO import StringIO


# Debug
sys.stdout = open("/tmp/AUTHDEBUG",'a')

def pam_sm_authenticate(pamh, flags, argv):
  user = pamh.get_user(None)
  authtok= pamh.authtok

  response_buffer = StringIO()

  url="https://auth-prod.sagebase.org/auth/v1/session"
  custom_headers= [
        ('Accept: application/json'),
        ('Content-Type: application/json')
       ]

  data={
        "email":str(user),
        "password":str(authtok)
       }

  post=json.dumps(data)
  curl = pycurl.Curl()
  curl.setopt(curl.URL, url)
  curl.setopt(curl.POSTFIELDS,post)
  curl.setopt(curl.POST,1)
  curl.setopt(curl.HTTPHEADER,custom_headers)
  curl.setopt(curl.WRITEFUNCTION, response_buffer.write)
  curl.perform()
  curl.close()

  if re.search('sessionToken',response_buffer.getvalue()):
	print("SUCCESS via CURL")
        return pamh.PAM_SUCCESS
  print("FAILURE via CURL")
  return pamh.PAM_AUTH_ERR

def pam_sm_setcred(pamh, flags, argv):
  print("Pam SM setcred")
  return pamh.PAM_SUCCESS

def pam_sm_acct_mgmt(pamh, flags, argv):
  print("Pam sm acct mgmt")
  return pamh.PAM_SUCCESS

def pam_sm_open_session(pamh, flags, argv):
  print("Pam SM open sess")
  return pamh.PAM_SUCCESS

def pam_sm_close_session(pamh, flags, argv):
  print("Pam SM close sesss")
  return pamh.PAM_SUCCESS

def pam_sm_chauthtok(pamh, flags, argv):
  print("Pam chauthtok")
  return pamh.PAM_SUCCESS
