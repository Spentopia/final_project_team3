import { useState } from "react";
import { Card } from "@/shared/ui/card";
import { Button } from "@/shared/ui/button";
import { Badge } from "@/shared/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/shared/ui/tabs";
import { Input } from "@/shared/ui/input";
import { Textarea } from "@/shared/ui/textarea";
import {
  MessageCircle,
  Heart,
  Trophy,
  Send,
  Image as ImageIcon,
  Sparkles,
} from "lucide-react";
import { toast } from "sonner";

interface Post {
  id: number;
  author: string;
  avatar: string;
  title: string;
  content: string;
  image?: string;
  likes: number;
  comments: number;
  timestamp: string;
}

const contestPosts: Post[] = [
  {
    id: 1,
    author: "패션왕",
    avatar: "🤩",
    title: "나만의 힙스터 룩!",
    content: "모자와 선글라스로 완성한 내 아바타 어때요?",
    likes: 234,
    comments: 45,
    timestamp: "2시간 전",
  },
  {
    id: 2,
    author: "큐티",
    avatar: "🥰",
    title: "핑크핑크 러블리 코디",
    content: "핑크 드레스에 왕관까지! 공주님 컨셉으로 만들어봤어요~",
    likes: 189,
    comments: 32,
    timestamp: "5시간 전",
  },
  {
    id: 3,
    author: "전사",
    avatar: "⚔️",
    title: "용사의 갑옷",
    content: "RPG 게임 주인공처럼 멋진 갑옷 세트!",
    likes: 156,
    comments: 28,
    timestamp: "1일 전",
  },
];

const communityPosts: Post[] = [
  {
    id: 101,
    author: "절약왕",
    avatar: "💰",
    title: "이번 달 50만원으로 살기 성공!",
    content: "식비 줄이기 꿀팁 공유해요. 집밥이 최고!",
    likes: 345,
    comments: 67,
    timestamp: "1시간 전",
  },
  {
    id: 102,
    author: "목표달성",
    avatar: "🎯",
    title: "드디어 1000만원 모았어요!",
    content: "2년 동안 꾸준히 저축한 결과예요. 여러분도 할 수 있어요!",
    likes: 567,
    comments: 89,
    timestamp: "3시간 전",
  },
];

export default function Community() {
  const [selectedTab, setSelectedTab] = useState("contest");
  const [newPost, setNewPost] = useState({
    title: "",
    content: "",
  });

  const handleLike = (postId: number) => {
    toast.success("좋아요를 눌렀어요! ❤️");
  };

  const handleSubmitPost = () => {
    if (!newPost.title || !newPost.content) {
      toast.error("제목과 내용을 입력해주세요");
      return;
    }
    toast.success("게시글이 등록되었습니다!");
    setNewPost({ title: "", content: "" });
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="mb-2 text-3xl font-bold text-gray-900">커뮤니티</h1>
          <p className="text-gray-600">다른 사용자들과 소통하고 경험을 나눠보세요</p>
        </div>
        <Button className="bg-gradient-to-r from-cyan-500 to-blue-500">
          <Send className="mr-2 h-4 w-4" />
          글쓰기
        </Button>
      </div>

      {/* AI Chatbot Banner */}
      <Card className="border-none bg-gradient-to-r from-cyan-500 to-blue-500 p-6 text-white backdrop-blur-xl">
        <div className="flex items-center gap-4">
          <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-full bg-white/20 text-3xl backdrop-blur-sm">
            🤖
          </div>
          <div className="flex-1">
            <h3 className="mb-1 font-bold">AI 챗봇 고객센터</h3>
            <p className="text-sm opacity-90">
              내 아바타가 상담원이 되어 궁금한 점을 답변해드려요!
            </p>
          </div>
          <Button variant="secondary">
            <MessageCircle className="mr-2 h-4 w-4" />
            채팅 시작
          </Button>
        </div>
      </Card>

      <Tabs value={selectedTab} onValueChange={setSelectedTab} className="space-y-6">
        <TabsList>
          <TabsTrigger value="contest">
            <Trophy className="mr-2 h-4 w-4" />
            아바타 콘테스트
          </TabsTrigger>
          <TabsTrigger value="community">
            <MessageCircle className="mr-2 h-4 w-4" />
            자유게시판
          </TabsTrigger>
          <TabsTrigger value="tips">
            <Sparkles className="mr-2 h-4 w-4" />
            절약 꿀팁
          </TabsTrigger>
        </TabsList>

        {/* Avatar Contest */}
        <TabsContent value="contest" className="space-y-6">
          <Card className="border-none bg-gradient-to-br from-amber-50 to-yellow-50 p-6 backdrop-blur-xl">
            <div className="flex items-center gap-3">
              <Trophy className="h-8 w-8 text-amber-600" />
              <div>
                <h3 className="font-bold text-gray-900">4월 아바타 콘테스트 진행중!</h3>
                <p className="text-sm text-gray-600">
                  좋아요를 가장 많이 받은 3명에게 특별 보상! (종료: 4월 30일)
                </p>
              </div>
            </div>
          </Card>

          <div className="grid gap-6 md:grid-cols-2">
            {contestPosts.map((post) => (
              <Card
                key={post.id}
                className="overflow-hidden border-none bg-white/80 backdrop-blur-xl transition-all hover:shadow-xl"
              >
                {/* Mock Avatar Display */}
                <div className="aspect-video bg-gradient-to-br from-purple-100 to-pink-100 p-8">
                  <div className="flex h-full items-center justify-center text-6xl">
                    {post.avatar}
                  </div>
                </div>

                <div className="p-6">
                  <div className="mb-4">
                    <div className="mb-2 flex items-center gap-2">
                      <span className="text-2xl">{post.avatar}</span>
                      <span className="font-bold text-gray-900">{post.author}</span>
                      <span className="text-sm text-gray-500">{post.timestamp}</span>
                    </div>
                    <h3 className="mb-2 font-bold text-gray-900">{post.title}</h3>
                    <p className="text-sm text-gray-600">{post.content}</p>
                  </div>

                  <div className="flex items-center gap-4">
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => handleLike(post.id)}
                      className="text-pink-600 hover:text-pink-700"
                    >
                      <Heart className="mr-2 h-4 w-4" />
                      {post.likes}
                    </Button>
                    <Button variant="ghost" size="sm">
                      <MessageCircle className="mr-2 h-4 w-4" />
                      {post.comments}
                    </Button>
                  </div>
                </div>
              </Card>
            ))}
          </div>
        </TabsContent>

        {/* Community Board */}
        <TabsContent value="community" className="space-y-6">
          {/* New Post Form */}
          <Card className="border-none bg-white/80 p-6 backdrop-blur-xl">
            <h3 className="mb-4 font-bold text-gray-900">새 글 작성</h3>
            <div className="space-y-4">
              <Input
                placeholder="제목을 입력하세요"
                value={newPost.title}
                onChange={(e) => setNewPost({ ...newPost, title: e.target.value })}
              />
              <Textarea
                placeholder="내용을 입력하세요"
                rows={4}
                value={newPost.content}
                onChange={(e) => setNewPost({ ...newPost, content: e.target.value })}
              />
              <div className="flex items-center justify-between">
                <Button variant="outline" size="sm">
                  <ImageIcon className="mr-2 h-4 w-4" />
                  이미지 첨부
                </Button>
                <Button
                  onClick={handleSubmitPost}
                  className="bg-gradient-to-r from-cyan-500 to-blue-500"
                >
                  게시하기
                </Button>
              </div>
            </div>
          </Card>

          {/* Posts */}
          <div className="space-y-4">
            {communityPosts.map((post) => (
              <Card
                key={post.id}
                className="border-none bg-white/80 p-6 backdrop-blur-xl transition-all hover:shadow-lg"
              >
                <div className="mb-4 flex items-start gap-3">
                  <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-purple-100 to-pink-100 text-2xl">
                    {post.avatar}
                  </div>
                  <div className="flex-1">
                    <div className="mb-1 flex items-center gap-2">
                      <span className="font-bold text-gray-900">{post.author}</span>
                      <span className="text-sm text-gray-500">{post.timestamp}</span>
                    </div>
                    <h3 className="mb-2 font-bold text-gray-900">{post.title}</h3>
                    <p className="text-gray-600">{post.content}</p>
                  </div>
                </div>

                <div className="flex items-center gap-4 border-t pt-4">
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => handleLike(post.id)}
                    className="text-pink-600 hover:text-pink-700"
                  >
                    <Heart className="mr-2 h-4 w-4" />
                    {post.likes}
                  </Button>
                  <Button variant="ghost" size="sm">
                    <MessageCircle className="mr-2 h-4 w-4" />
                    {post.comments}
                  </Button>
                </div>
              </Card>
            ))}
          </div>
        </TabsContent>

        {/* Tips */}
        <TabsContent value="tips" className="space-y-6">
          <div className="grid gap-6 md:grid-cols-2">
            <Card className="border-none bg-white/80 p-6 backdrop-blur-xl">
              <Badge className="mb-3 bg-green-500">식비 절약</Badge>
              <h3 className="mb-2 font-bold text-gray-900">집밥 도시락으로 월 20만원 절약!</h3>
              <p className="mb-4 text-sm text-gray-600">
                외식 대신 도시락을 싸면 한 달에 평균 20만원을 아낄 수 있어요. 주말에 미리 반찬을 준비하면 훨씬 편해요.
              </p>
              <div className="flex items-center justify-between text-sm text-gray-500">
                <span>절약왕 • 2일 전</span>
                <div className="flex items-center gap-2">
                  <Heart className="h-4 w-4" />
                  <span>234</span>
                </div>
              </div>
            </Card>

            <Card className="border-none bg-white/80 p-6 backdrop-blur-xl">
              <Badge className="mb-3 bg-blue-500">교통비 절약</Badge>
              <h3 className="mb-2 font-bold text-gray-900">대중교통 정기권으로 30% 절약</h3>
              <p className="mb-4 text-sm text-gray-600">
                매일 출퇴근하시나요? 정기권을 이용하면 교통비를 30% 이상 절약할 수 있어요. 한 달 정기권 강력 추천!
              </p>
              <div className="flex items-center justify-between text-sm text-gray-500">
                <span>알뜰맨 • 3일 전</span>
                <div className="flex items-center gap-2">
                  <Heart className="h-4 w-4" />
                  <span>189</span>
                </div>
              </div>
            </Card>

            <Card className="border-none bg-white/80 p-6 backdrop-blur-xl">
              <Badge className="mb-3 bg-cyan-500">구독 관리</Badge>
              <h3 className="mb-2 font-bold text-gray-900">안 쓰는 구독 정리로 월 5만원 아끼기</h3>
              <p className="mb-4 text-sm text-gray-600">
                OTT, 음악 스트리밍 등 안 쓰는 구독은 과감히 정리하세요. 정말 필요한 것만 남기면 월 5만원은 금방이에요.
              </p>
              <div className="flex items-center justify-between text-sm text-gray-500">
                <span>구독킬러 • 5일 전</span>
                <div className="flex items-center gap-2">
                  <Heart className="h-4 w-4" />
                  <span>156</span>
                </div>
              </div>
            </Card>

            <Card className="border-none bg-white/80 p-6 backdrop-blur-xl">
              <Badge className="mb-3 bg-amber-500">포인트 활용</Badge>
              <h3 className="mb-2 font-bold text-gray-900">카드 포인트 똑똑하게 사용하기</h3>
              <p className="mb-4 text-sm text-gray-600">
                쌓인 포인트는 할인 쿠폰으로 전환하세요. 현금처럼 사용할 수 있어서 실질적인 절약이 가능해요.
              </p>
              <div className="flex items-center justify-between text-sm text-gray-500">
                <span>포인트맨 • 1주일 전</span>
                <div className="flex items-center gap-2">
                  <Heart className="h-4 w-4" />
                  <span>198</span>
                </div>
              </div>
            </Card>
          </div>
        </TabsContent>
      </Tabs>
    </div>
  );
}
