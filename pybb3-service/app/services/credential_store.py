from __future__ import annotations
import base64,hashlib,json,os,tempfile
from pathlib import Path
from cryptography.fernet import Fernet,InvalidToken
from app.config import settings

class CredentialStore:
    def __init__(self):
        self.directory=Path(settings.CREDENTIAL_DIRECTORY)
        secret=settings.CREDENTIAL_ENCRYPTION_KEY
        self.fernet=Fernet(base64.urlsafe_b64encode(hashlib.sha256(secret.encode()).digest())) if secret else None
    def configured(self): return self.fernet is not None
    def _path(self,credential_id):
        if not credential_id.replace('-','').replace('_','').isalnum(): raise ValueError('Invalid credential id')
        return self.directory/f'{credential_id}.credential'
    def save(self,credential_id,credential):
        if not self.fernet: raise RuntimeError('PYBB3_CREDENTIAL_ENCRYPTION_KEY is not configured')
        self.directory.mkdir(parents=True,exist_ok=True)
        data=self.fernet.encrypt(json.dumps(credential,separators=(',',':')).encode())
        fd,name=tempfile.mkstemp(dir=self.directory,prefix='.credential-',text=False)
        try:
            os.write(fd,data);os.fchmod(fd,0o600);os.close(fd);fd=-1
            os.replace(name,self._path(credential_id))
        finally:
            if fd>=0:os.close(fd)
            if os.path.exists(name):os.unlink(name)
    def load(self,credential_id):
        if not self.fernet: raise RuntimeError('Credential encryption is not configured')
        try:return json.loads(self.fernet.decrypt(self._path(credential_id).read_bytes()).decode())
        except (FileNotFoundError,InvalidToken,json.JSONDecodeError) as error:raise KeyError('Credential missing or unreadable') from error
    def status(self,credential_id):
        path=self._path(credential_id)
        return {'credentialId':credential_id,'encryptionConfigured':self.configured(),'credentialConfigured':path.is_file(),'username':self.load(credential_id).get('username','') if self.configured() and path.is_file() else ''}
credential_store=CredentialStore()
