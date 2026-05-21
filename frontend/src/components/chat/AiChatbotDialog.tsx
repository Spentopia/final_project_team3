import { FormEvent, KeyboardEvent, useState } from "react";
import { Bot, Loader2, Send, User } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/shared/ui/dialog";
import { Button } from "@/shared/ui/button";
import { Textarea } from "@/shared/ui/textarea";
import { sendChatMessage } from "@/shared/api/chatApi";
import { cn } from "@/shared/ui/utils";
import { toast } from "sonner";

type ChatMessage = {
  id: number;
  role: "assistant" | "user";
  content: string;
};

type Props = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
};

const initialMessages: ChatMessage[] = [
  {
    id: 1,
    role: "assistant",
    content:
      "안녕하세요. 소비 기록, 예산 관리, 절약 방법, 영수증 인증에 대해 편하게 물어보세요.",
  },
];

export default function AiChatbotDialog({ open, onOpenChange }: Props) {
  const [messages, setMessages] = useState<ChatMessage[]>(initialMessages);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);

  const sendMessage = async () => {
    const message = input.trim();

    if (!message || loading) {
      return;
    }

    const userMessage: ChatMessage = {
      id: Date.now(),
      role: "user",
      content: message,
    };

    setMessages((prev) => [...prev, userMessage]);
    setInput("");
    setLoading(true);

    try {
      const data = await sendChatMessage({ message });
      const assistantMessage: ChatMessage = {
        id: Date.now() + 1,
        role: "assistant",
        content: data.response || "답변을 생성하지 못했어요.",
      };

      setMessages((prev) => [...prev, assistantMessage]);
    } catch (error) {
      const message =
        error instanceof Error ? error.message : "AI 상담 중 오류가 발생했습니다.";
      toast.error(message);
      setMessages((prev) => [
        ...prev,
        {
          id: Date.now() + 1,
          role: "assistant",
          content: "지금은 답변을 불러오지 못했어요. 잠시 후 다시 시도해주세요.",
        },
      ]);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault();
    void sendMessage();
  };

  const handleKeyDown = (event: KeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();
      void sendMessage();
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[calc(100vh-2rem)] max-w-[720px] gap-0 overflow-hidden border-sky-200/80 bg-white p-0 shadow-[0_18px_44px_rgba(37,99,235,0.14)] dark:border-violet-300/25 dark:bg-[#0b1020] dark:shadow-[0_18px_44px_rgba(124,58,237,0.18)]">
        <DialogHeader className="border-b border-sky-200/80 bg-[linear-gradient(135deg,#eff6ff,#dbeafe_46%,#bfdbfe)] p-5 text-slate-900 dark:border-violet-300/20 dark:bg-[linear-gradient(135deg,#0f172a,#2d1847_54%,#4c1d95)] dark:text-violet-50">
          <div className="flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-full border border-blue-200 bg-white/70 text-blue-700 shadow-[0_8px_20px_rgba(37,99,235,0.14)] dark:border-violet-300/30 dark:bg-white/10 dark:text-violet-100 dark:shadow-[0_8px_22px_rgba(124,58,237,0.24)]">
              <Bot className="h-6 w-6" />
            </div>
            <div>
              <DialogTitle>AI 챗바타 상담</DialogTitle>
              <DialogDescription className="text-slate-600 dark:text-violet-100/80">
                소비 고민을 짧게 남기면 바로 답변해드려요.
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        <div className="flex h-[420px] flex-col bg-gray-50 dark:bg-gray-950">
          <div className="flex-1 space-y-4 overflow-y-auto p-5">
            {messages.map((message) => {
              const isUser = message.role === "user";

              return (
                <div
                  key={message.id}
                  className={cn("flex gap-3", isUser && "justify-end")}
                >
                  {!isUser && (
                    <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-blue-100 text-blue-700 dark:bg-violet-950/60 dark:text-violet-200">
                      <Bot className="h-4 w-4" />
                    </div>
                  )}

                  <div
                    className={cn(
                      "max-w-[78%] whitespace-pre-wrap rounded-lg px-4 py-3 text-sm leading-relaxed",
                      isUser
                        ? "bg-[#2563eb] text-white shadow-[0_8px_20px_rgba(37,99,235,0.18)] dark:bg-[#2d1847] dark:text-violet-50 dark:shadow-[0_8px_20px_rgba(124,58,237,0.2)]"
                        : "border bg-white text-gray-800 dark:border-gray-800 dark:bg-gray-900 dark:text-gray-100",
                    )}
                  >
                    {message.content}
                  </div>

                  {isUser && (
                    <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-blue-100 text-blue-700 dark:bg-violet-950/60 dark:text-violet-200">
                      <User className="h-4 w-4" />
                    </div>
                  )}
                </div>
              );
            })}

            {loading && (
              <div className="flex gap-3">
                <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-blue-100 text-blue-700 dark:bg-violet-950/60 dark:text-violet-200">
                  <Bot className="h-4 w-4" />
                </div>
                <div className="flex items-center gap-2 rounded-lg border bg-white px-4 py-3 text-sm text-gray-600 dark:border-gray-800 dark:bg-gray-900 dark:text-gray-300">
                  <Loader2 className="h-4 w-4 animate-spin" />
                  답변 작성 중
                </div>
              </div>
            )}
          </div>

          <form onSubmit={handleSubmit} className="border-t bg-white p-4 dark:border-gray-800 dark:bg-gray-900">
            <div className="flex gap-3">
              <Textarea
                value={input}
                onChange={(event) => setInput(event.target.value)}
                onKeyDown={handleKeyDown}
                placeholder="예: 이번 달 식비를 줄이고 싶은데 어떻게 시작할까요?"
                rows={2}
                className="min-h-[52px] resize-none"
                disabled={loading}
              />
              <Button
                type="submit"
                className="h-[52px] bg-[linear-gradient(135deg,#3b82f6,#2563eb)] px-4 text-white shadow-lg shadow-blue-700/20 hover:brightness-105 dark:bg-[linear-gradient(135deg,#0f172a,#4338ca,#7c3aed)] dark:shadow-violet-900/30"
                disabled={loading || !input.trim()}
              >
                {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : <Send className="h-4 w-4" />}
              </Button>
            </div>
            <p className="mt-2 text-xs text-gray-500 dark:text-gray-400">
              Enter로 전송하고 Shift+Enter로 줄바꿈할 수 있어요.
            </p>
          </form>
        </div>
      </DialogContent>
    </Dialog>
  );
}
