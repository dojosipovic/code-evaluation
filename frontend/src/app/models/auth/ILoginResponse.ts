export interface ILoginResponse {
  status: 'AUTHENTICATED' | 'TWO_FACTOR_REQUIRED';
  accessToken: string | null;
  twoFactorToken: string | null;
  primaryMethod: 'totp' | 'webauthn' | null;
  availableMethods: string[];
}
