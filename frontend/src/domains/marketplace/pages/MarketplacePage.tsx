import { useState } from "react";
import { Card } from "@/shared/ui/card";
import { Button } from "@/shared/ui/button";
import { Badge } from "@/shared/ui/badge";
import { Input } from "@/shared/ui/input";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/shared/ui/tabs";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/ui/select";
import {
  Wallet,
  TrendingUp,
  ShoppingCart,
  Tag,
  Search,
  AlertCircle,
} from "lucide-react";
import { toast } from "sonner";

interface MarketItem {
  id: number;
  name: string;
  emoji: string;
  rarity: "common" | "rare" | "epic" | "legendary";
  price: number;
  seller: string;
  listedDate: string;
  category: "body" | "hair" | "face" | "clothes" | "accessory";
}

const marketItems: MarketItem[] = [
  {
    id: 1,
    name: "빨간머리",
    emoji: "👩‍🦰",
    rarity: "epic",
    price: 500,
    seller: "user123",
    listedDate: "2시간 전",
    category: "hair",
  },
  {
    id: 2,
    name: "왕관",
    emoji: "👑",
    rarity: "legendary",
    price: 1500,
    seller: "collector99",
    listedDate: "5시간 전",
    category: "accessory",
  },
  {
    id: 3,
    name: "갑옷",
    emoji: "🛡️",
    rarity: "legendary",
    price: 2000,
    seller: "warrior",
    listedDate: "1일 전",
    category: "clothes",
  },
  {
    id: 4,
    name: "하트 눈",
    emoji: "😍",
    rarity: "rare",
    price: 300,
    seller: "lovely_user",
    listedDate: "3시간 전",
    category: "face",
  },
  {
    id: 5,
    name: "날개",
    emoji: "🦋",
    rarity: "epic",
    price: 800,
    seller: "angel22",
    listedDate: "6시간 전",
    category: "accessory",
  },
  {
    id: 6,
    name: "곱슬머리",
    emoji: "🦱",
    rarity: "rare",
    price: 250,
    seller: "style_master",
    listedDate: "12시간 전",
    category: "hair",
  },
];

const myListings = [
  {
    id: 101,
    name: "정장",
    emoji: "👔",
    rarity: "rare" as const,
    price: 400,
    listedDate: "1일 전",
    views: 45,
  },
  {
    id: 102,
    name: "모자",
    emoji: "🎩",
    rarity: "common" as const,
    price: 150,
    listedDate: "2일 전",
    views: 28,
  },
];

const rarityColors = {
  common: "bg-gray-500",
  rare: "bg-blue-500",
  epic: "bg-cyan-500",
  legendary: "bg-amber-500",
};

const rarityLabels = {
  common: "일반",
  rare: "레어",
  epic: "에픽",
  legendary: "전설",
};

export default function Marketplace() {
  const [walletConnected] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [sortBy, setSortBy] = useState("latest");
  const [filterRarity, setFilterRarity] = useState("all");

  const handleBuy = (item: MarketItem) => {
    if (!walletConnected) {
      toast.error("지갑을 먼저 연결해주세요");
      return;
    }
    toast.success(
      <div>
        <p className="font-bold">{item.name} 구매 완료! 🎉</p>
        <p className="text-sm">-{item.price} SPT</p>
      </div>
    );
  };

  const handleCancelListing = (id: number) => {
    toast.success("판매가 취소되었습니다");
  };

  const filteredItems = marketItems
    .filter((item) => {
      if (searchQuery && !item.name.toLowerCase().includes(searchQuery.toLowerCase())) {
        return false;
      }
      if (filterRarity !== "all" && item.rarity !== filterRarity) {
        return false;
      }
      return true;
    })
    .sort((a, b) => {
      if (sortBy === "price-low") return a.price - b.price;
      if (sortBy === "price-high") return b.price - a.price;
      return 0; // latest
    });

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="mb-2 text-3xl font-bold text-gray-900 dark:text-gray-100">NFT 마켓플레이스</h1>
          <p className="text-gray-600 dark:text-gray-300">아바타 아이템을 자유롭게 거래해보세요</p>
        </div>
      </div>

      {/* Wallet Status Banner */}
      {!walletConnected && (
        <Card className="border-amber-500 bg-gradient-to-r from-amber-50 to-yellow-50 p-4">
          <div className="flex items-start gap-3">
            <AlertCircle className="mt-0.5 h-5 w-5 shrink-0 text-amber-600" />
            <div>
              <h3 className="font-bold text-amber-900">지갑 연결이 필요해요</h3>
              <p className="text-sm text-amber-700">
                지갑을 연결하면 아이템을 NFT로 발행하고 거래할 수 있어요. 지갑이 없어도 개인 소장은 가능합니다.
              </p>
            </div>
          </div>
        </Card>
      )}

      {/* Stats */}
      <div className="grid gap-6 md:grid-cols-4">
        <Card className="border-none bg-white/80 p-6 backdrop-blur-xl">
          <div className="mb-2 flex items-center gap-2 text-gray-600">
            <ShoppingCart className="h-4 w-4" />
            <span className="text-sm">총 거래량</span>
          </div>
          <p className="mb-1 text-3xl font-bold text-gray-900">12,345</p>
          <p className="text-sm text-green-600">+15% 이번 주</p>
        </Card>

        <Card className="border-none bg-white/80 p-6 backdrop-blur-xl">
          <div className="mb-2 flex items-center gap-2 text-gray-600">
            <Tag className="h-4 w-4" />
            <span className="text-sm">등록된 아이템</span>
          </div>
          <p className="mb-1 text-3xl font-bold text-gray-900">248</p>
          <p className="text-sm text-gray-600">현재 판매중</p>
        </Card>

        <Card className="border-none bg-white/80 p-6 backdrop-blur-xl">
          <div className="mb-2 flex items-center gap-2 text-gray-600">
            <TrendingUp className="h-4 w-4" />
            <span className="text-sm">평균 거래가</span>
          </div>
          <p className="mb-1 text-3xl font-bold text-gray-900">650 SPT</p>
          <p className="text-sm text-green-600">+8% 상승</p>
        </Card>

        <Card className="border-none bg-gradient-to-br from-cyan-500 to-blue-500 p-6 text-white backdrop-blur-xl">
          <div className="mb-2 flex items-center gap-2">
            <Wallet className="h-4 w-4" />
            <span className="text-sm">내 잔액</span>
          </div>
          <p className="mb-1 text-3xl font-bold">1,250 SPT</p>
          <p className="text-sm opacity-90">사용 가능</p>
        </Card>
      </div>

      <Tabs defaultValue="market" className="space-y-6">
        <TabsList>
          <TabsTrigger value="market">마켓</TabsTrigger>
          <TabsTrigger value="my-listings">내 판매 목록</TabsTrigger>
        </TabsList>

        {/* Market Tab */}
        <TabsContent value="market" className="space-y-6">
          {/* Filters */}
          <Card className="border-none bg-white/80 p-4 backdrop-blur-xl">
            <div className="flex flex-wrap gap-4">
              <div className="flex-1">
                <div className="relative">
                  <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                  <Input
                    placeholder="아이템 검색..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className="pl-10"
                  />
                </div>
              </div>

              <Select value={filterRarity} onValueChange={setFilterRarity}>
                <SelectTrigger className="w-[180px]">
                  <SelectValue placeholder="희귀도" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">전체 희귀도</SelectItem>
                  <SelectItem value="common">일반</SelectItem>
                  <SelectItem value="rare">레어</SelectItem>
                  <SelectItem value="epic">에픽</SelectItem>
                  <SelectItem value="legendary">전설</SelectItem>
                </SelectContent>
              </Select>

              <Select value={sortBy} onValueChange={setSortBy}>
                <SelectTrigger className="w-[180px]">
                  <SelectValue placeholder="정렬" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="latest">최신순</SelectItem>
                  <SelectItem value="price-low">낮은 가격순</SelectItem>
                  <SelectItem value="price-high">높은 가격순</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </Card>

          {/* Market Items */}
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {filteredItems.map((item) => (
              <Card
                key={item.id}
                className="overflow-hidden border-none bg-white/80 backdrop-blur-xl transition-all hover:shadow-xl"
              >
                <div className="aspect-square bg-gradient-to-br from-purple-100 to-pink-100 p-8">
                  <div className="flex h-full items-center justify-center text-8xl">
                    {item.emoji}
                  </div>
                </div>
                <div className="p-4">
                  <div className="mb-3 flex items-start justify-between">
                    <div>
                      <h3 className="mb-1 font-bold text-gray-900">{item.name}</h3>
                      <p className="text-xs text-gray-600">판매자: {item.seller}</p>
                    </div>
                    <Badge className={rarityColors[item.rarity]}>
                      {rarityLabels[item.rarity]}
                    </Badge>
                  </div>

                  <div className="mb-3 flex items-center justify-between">
                    <div className="flex items-center gap-1">
                      <div className="flex h-6 w-6 items-center justify-center rounded-full bg-gradient-to-br from-amber-400 to-yellow-400">
                        <span className="text-xs font-bold text-white">S</span>
                      </div>
                      <span className="font-bold text-gray-900">{item.price} SPT</span>
                    </div>
                    <span className="text-xs text-gray-500">{item.listedDate}</span>
                  </div>

                  <Button
                    onClick={() => handleBuy(item)}
                    className="w-full bg-gradient-to-r from-cyan-500 to-blue-500"
                    size="sm"
                  >
                    구매하기
                  </Button>
                </div>
              </Card>
            ))}
          </div>

          {filteredItems.length === 0 && (
            <Card className="border-none bg-white/80 p-12 text-center backdrop-blur-xl">
              <p className="text-gray-500">검색 결과가 없어요</p>
            </Card>
          )}
        </TabsContent>

        {/* My Listings Tab */}
        <TabsContent value="my-listings" className="space-y-6">
          {myListings.length > 0 ? (
            <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
              {myListings.map((item) => (
                <Card
                  key={item.id}
                  className="overflow-hidden border-none bg-white/80 backdrop-blur-xl"
                >
                  <div className="aspect-square bg-gradient-to-br from-blue-100 to-cyan-100 p-8">
                    <div className="flex h-full items-center justify-center text-8xl">
                      {item.emoji}
                    </div>
                  </div>
                  <div className="p-4">
                    <div className="mb-3 flex items-start justify-between">
                      <div>
                        <h3 className="mb-1 font-bold text-gray-900">{item.name}</h3>
                        <p className="text-xs text-gray-600">조회수: {item.views}회</p>
                      </div>
                      <Badge className={rarityColors[item.rarity]}>
                        {rarityLabels[item.rarity]}
                      </Badge>
                    </div>

                    <div className="mb-3 flex items-center justify-between">
                      <div className="flex items-center gap-1">
                        <div className="flex h-6 w-6 items-center justify-center rounded-full bg-gradient-to-br from-amber-400 to-yellow-400">
                          <span className="text-xs font-bold text-white">S</span>
                        </div>
                        <span className="font-bold text-gray-900">{item.price} SPT</span>
                      </div>
                      <span className="text-xs text-gray-500">{item.listedDate}</span>
                    </div>

                    <Button
                      onClick={() => handleCancelListing(item.id)}
                      variant="outline"
                      className="w-full"
                      size="sm"
                    >
                      판매 취소
                    </Button>
                  </div>
                </Card>
              ))}
            </div>
          ) : (
            <Card className="border-none bg-white/80 p-12 text-center backdrop-blur-xl">
              <p className="mb-4 text-gray-500">판매중인 아이템이 없어요</p>
              <Button className="bg-gradient-to-r from-cyan-500 to-blue-500">
                아이템 판매하기
              </Button>
            </Card>
          )}

          {/* Transaction Info */}
          <Card className="border-none bg-gradient-to-br from-purple-50 to-pink-50 p-6 backdrop-blur-xl">
            <h3 className="mb-4 font-bold text-gray-900">거래 수수료 안내</h3>
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-gray-700">판매 수수료</span>
                <span className="font-bold text-gray-900">5%</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-gray-700">블록체인 가스비</span>
                <span className="font-bold text-gray-900">구매자 부담</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-gray-700">최소 판매가</span>
                <span className="font-bold text-gray-900">100 SPT</span>
              </div>
              <div className="mt-4 rounded-lg border border-cyan-200 bg-white p-3 text-sm text-gray-600">
                💡 판매 취소는 등록 후 24시간 이내에만 가능해요
              </div>
            </div>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
}
