const PASSWORD_RULE =
  /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*()_+\-=\[\]{};':"\\|<>?,./`~]).{8,}$/;

export const PASSWORD_REQUIREMENTS_MESSAGE =
  "비밀번호는 영문 대소문자, 숫자, 특수문자를 모두 포함하여 8자 이상이어야 합니다.";

export function validatePassword(password: string): string | null {
  if (!PASSWORD_RULE.test(password)) {
    return PASSWORD_REQUIREMENTS_MESSAGE;
  }

  return null;
}
