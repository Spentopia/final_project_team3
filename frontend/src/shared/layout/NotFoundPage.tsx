import { Link } from "react-router";
import { Button } from "../ui/button";
import { Home, Search } from "lucide-react";

export default function NotFoundPage() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-purple-500 via-pink-500 to-blue-500 p-4">
      <div className="text-center text-white">
        <div className="mb-8">
          <h1 className="mb-4 text-9xl font-bold">404</h1>
          <h2 className="mb-2 text-3xl font-bold">페이지를 찾을 수 없어요</h2>
          <p className="text-lg opacity-90">
            요청하신 페이지가 존재하지 않거나 이동되었을 수 있어요
          </p>
        </div>

        <div className="mb-8 flex justify-center gap-2 text-6xl">
          <span>😢</span>
        </div>

        <div className="flex justify-center gap-3">
          <Link to="/">
            <Button size="lg" variant="secondary">
              <Home className="mr-2 h-5 w-5" />
              홈으로 돌아가기
            </Button>
          </Link>
          <Button size="lg" variant="outline" className="border-white text-white hover:bg-white/20">
            <Search className="mr-2 h-5 w-5" />
            도움말 검색
          </Button>
        </div>
      </div>
    </div>
  );
}
