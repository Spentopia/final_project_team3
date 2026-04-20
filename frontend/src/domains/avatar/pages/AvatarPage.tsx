import { useState } from "react";
import { Card } from "@/shared/ui/card";
import { Button } from "@/shared/ui/button";
import { Badge } from "@/shared/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/shared/ui/tabs";
import {
  Download,
  Share2,
  Sparkles,
  Lock,
  CheckCircle,
  Camera,
  Shuffle,
} from "lucide-react";
import { toast } from "sonner";

interface AvatarItem {
  id: number;
  type: "body" | "hair" | "face" | "clothes" | "accessory";
  name: string;
  emoji: string;
  owned: boolean;
  rarity: "common" | "rare" | "epic" | "legendary";
}

const avatarItems: AvatarItem[] = [
  // Body
  { id: 1, type: "body", name: "기본 몸", emoji: "🧍", owned: true, rarity: "common" },
  { id: 2, type: "body", name: "근육질", emoji: "💪", owned: true, rarity: "rare" },
  { id: 3, type: "body", name: "날씬이", emoji: "🏃", owned: false, rarity: "rare" },
  
  // Hair
  { id: 10, type: "hair", name: "기본 헤어", emoji: "👦", owned: true, rarity: "common" },
  { id: 11, type: "hair", name: "긴 머리", emoji: "👩", owned: true, rarity: "common" },
  { id: 12, type: "hair", name: "곱슬머리", emoji: "🦱", owned: false, rarity: "rare" },
  { id: 13, type: "hair", name: "빨간머리", emoji: "👩‍🦰", owned: false, rarity: "epic" },
  
  // Face
  { id: 20, type: "face", name: "미소", emoji: "😊", owned: true, rarity: "common" },
  { id: 21, type: "face", name: "쿨", emoji: "😎", owned: true, rarity: "common" },
  { id: 22, type: "face", name: "하트 눈", emoji: "😍", owned: false, rarity: "rare" },
  { id: 23, type: "face", name: "별 눈", emoji: "🤩", owned: false, rarity: "epic" },
  
  // Clothes
  { id: 30, type: "clothes", name: "티셔츠", emoji: "👕", owned: true, rarity: "common" },
  { id: 31, type: "clothes", name: "정장", emoji: "👔", owned: true, rarity: "rare" },
  { id: 32, type: "clothes", name: "드레스", emoji: "👗", owned: false, rarity: "epic" },
  { id: 33, type: "clothes", name: "갑옷", emoji: "🛡️", owned: false, rarity: "legendary" },
  
  // Accessory
  { id: 40, type: "accessory", name: "없음", emoji: "✨", owned: true, rarity: "common" },
  { id: 41, type: "accessory", name: "모자", emoji: "🎩", owned: true, rarity: "common" },
  { id: 42, type: "accessory", name: "왕관", emoji: "👑", owned: false, rarity: "legendary" },
  { id: 43, type: "accessory", name: "날개", emoji: "🦋", owned: false, rarity: "epic" },
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

export default function Avatar() {
  const [selectedItems, setSelectedItems] = useState({
    body: 1,
    hair: 10,
    face: 20,
    clothes: 30,
    accessory: 40,
  });

  const handleItemSelect = (item: AvatarItem) => {
    if (!item.owned) {
      toast.error("아직 소유하지 않은 아이템이에요");
      return;
    }
    setSelectedItems({
      ...selectedItems,
      [item.type]: item.id,
    });
    toast.success(`${item.name}(을)를 착용했어요!`);
  };

  const handleScreenshot = () => {
    toast.success("아바타 스크린샷이 저장되었어요! 📸");
  };

  const handleRandomize = () => {
    const ownedItems = avatarItems.filter((item) => item.owned);
    const randomBody = ownedItems.find((item) => item.type === "body");
    const randomHair = ownedItems.find((item) => item.type === "hair");
    const randomFace = ownedItems.find((item) => item.type === "face");
    const randomClothes = ownedItems.find((item) => item.type === "clothes");
    const randomAccessory = ownedItems.find((item) => item.type === "accessory");

    setSelectedItems({
      body: randomBody?.id || selectedItems.body,
      hair: randomHair?.id || selectedItems.hair,
      face: randomFace?.id || selectedItems.face,
      clothes: randomClothes?.id || selectedItems.clothes,
      accessory: randomAccessory?.id || selectedItems.accessory,
    });
    toast.success("랜덤 코디 완료!");
  };

  const getSelectedItem = (type: keyof typeof selectedItems) => {
    return avatarItems.find((item) => item.id === selectedItems[type]);
  };

  const ownedCount = avatarItems.filter((item) => item.owned).length;
  const totalCount = avatarItems.length;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="mb-2 text-3xl font-bold text-gray-900 dark:text-gray-100">내 아바타</h1>
          <p className="text-gray-600 dark:text-gray-300">
            보유 아이템: {ownedCount}/{totalCount}
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={handleRandomize}>
            <Shuffle className="mr-2 h-4 w-4" />
            랜덤 코디
          </Button>
          <Button variant="outline" onClick={handleScreenshot}>
            <Camera className="mr-2 h-4 w-4" />
            스크린샷
          </Button>
          <Button className="bg-gradient-to-r from-cyan-500 to-blue-500">
            <Share2 className="mr-2 h-4 w-4" />
            공유하기
          </Button>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-[400px_1fr]">
        {/* Avatar Preview */}
        <Card className="border-none bg-gradient-to-br from-cyan-500 to-blue-500 p-6 text-white backdrop-blur-xl">
          <h3 className="mb-4 font-bold">미리보기</h3>
          
          {/* Avatar Display */}
          <div className="mb-6 flex aspect-square items-center justify-center rounded-2xl bg-white/20 backdrop-blur-sm">
            <div className="text-center">
              <div className="mb-4 flex justify-center gap-2 text-6xl">
                {getSelectedItem("body")?.emoji}
              </div>
              <div className="mb-4 flex justify-center gap-2 text-6xl">
                {getSelectedItem("hair")?.emoji}
              </div>
              <div className="mb-4 flex justify-center gap-2 text-6xl">
                {getSelectedItem("face")?.emoji}
              </div>
              <div className="mb-4 flex justify-center gap-2 text-6xl">
                {getSelectedItem("clothes")?.emoji}
              </div>
              <div className="flex justify-center gap-2 text-6xl">
                {getSelectedItem("accessory")?.emoji}
              </div>
            </div>
          </div>

          {/* Stats */}
          <div className="space-y-3 rounded-lg bg-white/10 p-4 backdrop-blur-sm">
            <div className="flex items-center justify-between">
              <span>총 희귀도</span>
              <Badge className="bg-white/20">에픽</Badge>
            </div>
            <div className="flex items-center justify-between">
              <span>착용 아이템</span>
              <span className="font-bold">5개</span>
            </div>
            <div className="flex items-center justify-between">
              <span>획득 날짜</span>
              <span className="font-bold">2026.04.08</span>
            </div>
          </div>

          <div className="mt-4 rounded-lg bg-white/10 p-4 backdrop-blur-sm">
            <p className="mb-2 font-bold">🎁 다음 보상까지</p>
            <div className="mb-2 h-2 overflow-hidden rounded-full bg-white/20">
              <div className="h-full w-[65%] bg-white"></div>
            </div>
            <p className="text-sm">성실도 점수 25점만 더 모으면 랜덤 아바타!</p>
          </div>
        </Card>

        {/* Item Selection */}
        <Card className="border-none bg-white/80 p-6 backdrop-blur-xl">
          <Tabs defaultValue="all" className="space-y-6">
            <TabsList>
              <TabsTrigger value="all">전체</TabsTrigger>
              <TabsTrigger value="body">몸</TabsTrigger>
              <TabsTrigger value="hair">헤어</TabsTrigger>
              <TabsTrigger value="face">표정</TabsTrigger>
              <TabsTrigger value="clothes">옷</TabsTrigger>
              <TabsTrigger value="accessory">액세서리</TabsTrigger>
            </TabsList>

            <TabsContent value="all" className="space-y-6">
              {["body", "hair", "face", "clothes", "accessory"].map((type) => (
                <div key={type}>
                  <h3 className="mb-3 font-bold capitalize text-gray-900">
                    {type === "body" && "몸"}
                    {type === "hair" && "헤어"}
                    {type === "face" && "표정"}
                    {type === "clothes" && "옷"}
                    {type === "accessory" && "액세서리"}
                  </h3>
                  <div className="grid grid-cols-4 gap-3 sm:grid-cols-6">
                    {avatarItems
                      .filter((item) => item.type === type)
                      .map((item) => (
                        <button
                          key={item.id}
                          onClick={() => handleItemSelect(item)}
                          disabled={!item.owned}
                          className={`relative flex aspect-square flex-col items-center justify-center rounded-lg border-2 p-3 transition-all ${
                            selectedItems[item.type as keyof typeof selectedItems] === item.id
                              ? "border-cyan-500 bg-cyan-50 shadow-lg"
                              : item.owned
                              ? "border-gray-200 hover:border-cyan-300 hover:bg-gray-50"
                              : "cursor-not-allowed border-gray-200 bg-gray-100 opacity-50"
                          }`}
                        >
                          {!item.owned && (
                            <div className="absolute inset-0 flex items-center justify-center rounded-lg bg-black/50">
                              <Lock className="h-6 w-6 text-white" />
                            </div>
                          )}
                          <span className="mb-1 text-3xl">{item.emoji}</span>
                          <span className="text-xs text-gray-700">{item.name}</span>
                          <Badge
                            className={`mt-1 h-4 text-xs ${rarityColors[item.rarity]}`}
                          >
                            {rarityLabels[item.rarity]}
                          </Badge>
                        </button>
                      ))}
                  </div>
                </div>
              ))}
            </TabsContent>

            {(["body", "hair", "face", "clothes", "accessory"] as const).map((type) => (
              <TabsContent key={type} value={type}>
                <div className="grid grid-cols-4 gap-4 sm:grid-cols-6">
                  {avatarItems
                    .filter((item) => item.type === type)
                    .map((item) => (
                      <button
                        key={item.id}
                        onClick={() => handleItemSelect(item)}
                        disabled={!item.owned}
                        className={`relative flex aspect-square flex-col items-center justify-center rounded-lg border-2 p-4 transition-all ${
                          selectedItems[type] === item.id
                            ? "border-cyan-500 bg-cyan-50 shadow-lg"
                            : item.owned
                            ? "border-gray-200 hover:border-cyan-300 hover:bg-gray-50"
                            : "cursor-not-allowed border-gray-200 bg-gray-100 opacity-50"
                        }`}
                      >
                        {!item.owned && (
                          <div className="absolute inset-0 flex items-center justify-center rounded-lg bg-black/50">
                            <Lock className="h-8 w-8 text-white" />
                          </div>
                        )}
                        <span className="mb-2 text-4xl">{item.emoji}</span>
                        <span className="text-sm text-gray-700">{item.name}</span>
                        <Badge
                          className={`mt-2 h-5 text-xs ${rarityColors[item.rarity]}`}
                        >
                          {rarityLabels[item.rarity]}
                        </Badge>
                      </button>
                    ))}
                </div>
              </TabsContent>
            ))}
          </Tabs>
        </Card>
      </div>

      {/* Collection Progress */}
      <Card className="border-none bg-white/80 p-6 backdrop-blur-xl">
        <h3 className="mb-6 font-bold text-gray-900">컬렉션 진행도</h3>
        <div className="grid gap-6 md:grid-cols-4">
          <div className="rounded-lg border bg-gradient-to-br from-gray-50 to-gray-100 p-4">
            <div className="mb-2 flex items-center justify-between">
              <span className="font-bold text-gray-900">일반</span>
              <span className="text-sm text-gray-600">8/10</span>
            </div>
            <div className="h-2 overflow-hidden rounded-full bg-gray-200">
              <div className="h-full w-[80%] bg-gray-500"></div>
            </div>
          </div>

          <div className="rounded-lg border bg-gradient-to-br from-blue-50 to-blue-100 p-4">
            <div className="mb-2 flex items-center justify-between">
              <span className="font-bold text-blue-900">레어</span>
              <span className="text-sm text-blue-700">5/8</span>
            </div>
            <div className="h-2 overflow-hidden rounded-full bg-blue-200">
              <div className="h-full w-[62.5%] bg-blue-500"></div>
            </div>
          </div>

          <div className="rounded-lg border bg-gradient-to-br from-purple-50 to-purple-100 p-4">
            <div className="mb-2 flex items-center justify-between">
              <span className="font-bold text-purple-900">에픽</span>
              <span className="text-sm text-purple-700">2/6</span>
            </div>
            <div className="h-2 overflow-hidden rounded-full bg-cyan-200">
              <div className="h-full w-[33.33%] bg-cyan-500"></div>
            </div>
          </div>

          <div className="rounded-lg border bg-gradient-to-br from-amber-50 to-amber-100 p-4">
            <div className="mb-2 flex items-center justify-between">
              <span className="font-bold text-amber-900">전설</span>
              <span className="text-sm text-amber-700">0/3</span>
            </div>
            <div className="h-2 overflow-hidden rounded-full bg-amber-200">
              <div className="h-full w-[0%] bg-amber-500"></div>
            </div>
          </div>
        </div>
      </Card>

      {/* How to Get Items */}
      <Card className="border-none bg-gradient-to-br from-purple-50 to-pink-50 p-6 backdrop-blur-xl">
        <div className="mb-4 flex items-center gap-2">
          <Sparkles className="h-5 w-5 text-cyan-600" />
          <h3 className="font-bold text-gray-900">아이템 획득 방법</h3>
        </div>
        <div className="grid gap-4 md:grid-cols-3">
          <div className="rounded-lg border border-cyan-200 bg-white p-4">
            <CheckCircle className="mb-2 h-8 w-8 text-green-500" />
            <h4 className="mb-1 font-bold text-gray-900">성실도 보상</h4>
            <p className="text-sm text-gray-600">
              주간 성실도 70점 이상 달성 시 랜덤 아바타 지급
            </p>
          </div>

          <div className="rounded-lg border border-cyan-200 bg-white p-4">
            <Download className="mb-2 h-8 w-8 text-blue-500" />
            <h4 className="mb-1 font-bold text-gray-900">NFT 마켓</h4>
            <p className="text-sm text-gray-600">
              다른 유저와 아이템을 SPT로 거래할 수 있어요
            </p>
          </div>

          <div className="rounded-lg border border-cyan-200 bg-white p-4">
            <Sparkles className="mb-2 h-8 w-8 text-purple-500" />
            <h4 className="mb-1 font-bold text-gray-900">특별 이벤트</h4>
            <p className="text-sm text-gray-600">
              시즌 이벤트와 콘테스트에서 한정 아이템 획득
            </p>
          </div>
        </div>
      </Card>
    </div>
  );
}
