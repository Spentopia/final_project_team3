import { useState } from "react";
import { Eye, EyeOff } from "lucide-react";
import { Input } from "@/shared/ui/input";
import { Button } from "@/shared/ui/button";

type PasswordInputProps = Omit<
  React.ComponentProps<typeof Input>,
  "type"
>;

export default function PasswordInput({
  className,
  ...props
}: PasswordInputProps) {
  const [visible, setVisible] = useState(false);

  return (
    <div className="relative">
      <Input
        {...props}
        type={visible ? "text" : "password"}
        className={`pr-10 ${className ?? ""}`.trim()}
      />
      <Button
        type="button"
        variant="ghost"
        size="icon"
        className="absolute right-1 top-1/2 h-7 w-7 -translate-y-1/2 text-gray-500 hover:bg-transparent hover:text-gray-700"
        onClick={() => setVisible((prev) => !prev)}
        aria-label={visible ? "비밀번호 숨기기" : "비밀번호 보기"}
      >
        {visible ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
      </Button>
    </div>
  );
}
