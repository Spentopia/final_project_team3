const TOKEN_KEY = "spentopia_access_token";

export const authStorage = {
  setToken(token: string) {
    localStorage.setItem(TOKEN_KEY, token);
  },

  getToken() {
    return localStorage.getItem(TOKEN_KEY);
  },

  clear() {
    localStorage.removeItem("spentopia_access_token");
    localStorage.removeItem("spentopia_auth");
    sessionStorage.removeItem("spentopia_auth");
  },

  isLoggedIn() {
    return (
      !!localStorage.getItem(TOKEN_KEY) ||
      !!localStorage.getItem("spentopia_auth")
    );
  },
};