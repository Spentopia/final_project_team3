export const EMAIL_REQUIREMENTS_MESSAGE =
  "올바른 이메일 형식을 입력해주세요.";

const EMAIL_RULE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function validateEmail(email: string): string | null {
  if (!EMAIL_RULE.test(email.trim())) {
    return EMAIL_REQUIREMENTS_MESSAGE;
  }

  return null;
}
