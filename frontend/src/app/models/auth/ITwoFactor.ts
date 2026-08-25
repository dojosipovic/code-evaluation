export interface ITotpSetupResponse {
  secret: string;
  otpauthUrl: string;
}

export interface IWebAuthnOptionsResponse {
  token: string;
  optionsJson: string;
}

export interface IWebAuthnCredential {
  id: number;
  credentialId: string;
  discoverable: boolean | null;
  backedUp: boolean | null;
  createdAt: string;
  lastUsedAt: string | null;
}

export interface ITwoFactorSettings {
  totpEnabled: boolean;
  webauthnEnabled: boolean;
  webauthnCredentials: IWebAuthnCredential[];
}
