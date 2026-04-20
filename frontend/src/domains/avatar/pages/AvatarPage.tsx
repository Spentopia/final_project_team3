// ============================================================
// AvatarPage.tsx — 내 아바타 아이템 목록 페이지
//
// 구조:
//   - useAvatarItems 훅에서 데이터/액션 전부 가져옴
//   - 카테고리 탭 필터 → 아이템 카드 그리드
//   - 카드 클릭 → 상세 Dialog (NFT 민팅 버튼 포함)
//   - 로딩: Skeleton 카드 / 에러: 재시도 버튼
//
// CSS: AvatarPage.module.css 로 전부 분리
// import 경로: @/shared/ui/...
// ============================================================

import { useState } from "react";

// shadcn/ui 컴포넌트 — 프로젝트 실제 경로 @/shared/ui
import { Card, CardContent } from "@/shared/ui/card";
import { Badge } from "@/shared/ui/badge";
import { Button } from "@/shared/ui/button";
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogDescription,
} from "@/shared/ui/dialog";
import { Tabs, TabsList, TabsTrigger } from "@/shared/ui/tabs";
import { Skeleton } from "@/shared/ui/skeleton";

// 훅 — 아이템 목록 조회 + 민팅/전송 액션
import { useAvatarItems } from "../hooks/useAvatarItems";

// 타입 — 이 파일에서 UserItemResponse 구조를 참조해야 하므로 import
import type { UserItemResponse } from "../model/types";

// CSS Module — styles 객체로 모든 클래스 참조
// 빌드 시 클래스명이 해시로 변환되어 전역 충돌 방지
import styles from "./AvatarPage.module.css";

// ────────────────────────────────────────────────────────────
// 탭 필터 타입
// "all"은 UserItemResponse["category"] 유니온에 없으므로 별도 추가
// ────────────────────────────────────────────────────────────
type TabValue = "all" | UserItemResponse["category"];

// ────────────────────────────────────────────────────────────
// 레어리티 → CSS Module 클래스명 매핑
//
// 왜 Record<rarity, string> 형태로?
//   - rarityClassMap[item.rarity] 처럼 타입 안전하게 접근 가능
//   - styles["rarityCommon"] 같은 문자열 인덱싱보다 자동완성 지원
//
// 왜 컴포넌트 외부에 선언?
//   - 렌더링마다 새 객체 생성 방지 (참조 안정)
//   - 여러 컴포넌트에서 공유 가능
// ────────────────────────────────────────────────────────────
const rarityClassMap: Record<UserItemResponse["rarity"], string> = {
    common: styles.rarityCommon,
    rare: styles.rarityRare,
    epic: styles.rarityEpic,
};

const rarityCardClassMap: Record<UserItemResponse["rarity"], string> = {
    common: "",
    rare: styles.itemCardRare,
    epic: styles.itemCardEpic,
};

const rarityPlaceholderClassMap: Record<UserItemResponse["rarity"], string> = {
    common: styles.imagePlaceholderCommon,
    rare: styles.imagePlaceholderRare,
    epic: styles.imagePlaceholderEpic,
};

const rarityEmoji: Record<UserItemResponse["rarity"], string> = {
    common: "🎨",
    rare: "💎",
    epic: "✨",
};

const rarityLabel: Record<UserItemResponse["rarity"], string> = {
    common: "일반",
    rare: "레어",
    epic: "에픽",
};

const categoryLabel: Record<UserItemResponse["category"], string> = {
    background: "배경",
    frame: "프레임",
    effect: "효과",
    motion: "모션",
};

// ────────────────────────────────────────────────────────────
// ItemCard 컴포넌트
//
// 분리한 이유:
//   - AvatarPage가 너무 길어지는 것 방지 (관심사 분리)
//   - 카드 하나의 렌더링 로직을 독립적으로 파악 가능
//   - 추후 다른 페이지에서 재사용 가능
// ────────────────────────────────────────────────────────────
interface ItemCardProps {
    item: UserItemResponse;
    onClick: () => void; // 클릭 시 부모에서 selectedItem을 set → Dialog 오픈
}

function ItemCard({ item, onClick }: ItemCardProps) {
    const cardClass = [
        styles.itemCard,
        rarityCardClassMap[item.rarity],
        item.is_equipped ? styles.itemCardEquipped : "",
        item.is_nft ? styles.itemCardNft : "",
    ].join(" ");

    return (
        <Card className={cardClass} onClick={onClick}>
            <CardContent className={styles.cardContent}>

                {/* ── 아이템 이미지 ── */}
                <div className={styles.imageWrapper}>
                    {item.image_url ? (
                        <img src={item.image_url} alt={item.name} className={styles.itemImage} />
                    ) : (
                        <div className={rarityPlaceholderClassMap[item.rarity]}>
                            {rarityEmoji[item.rarity]}
                        </div>
                    )}
                    {item.is_nft && <span className={styles.nftOverlay}>NFT</span>}
                </div>

                {/* ── 아이템 이름 ── */}
                {/* overflow: hidden + text-overflow: ellipsis → CSS에서 처리 */}
                <p className={styles.itemName}>{item.name}</p>

                {/* ── 배지 영역 ── */}
                <div className={styles.badgeRow}>
                    {/* 레어리티 배지 — rarityClassMap으로 동적 클래스 적용 */}
                    <Badge className={rarityClassMap[item.rarity]}>
                        {rarityLabel[item.rarity]}
                    </Badge>

                    {/* 카테고리 배지 — outline variant는 shadcn 기본 스타일 사용 */}
                    <Badge variant="outline">{categoryLabel[item.category]}</Badge>

                    {/* NFT 배지 — is_nft가 true일 때만 렌더링 */}
                    {item.is_nft && (
                        <Badge className={styles.badgeNft}>NFT</Badge>
                    )}

                    {/* 장착중 배지 — is_equipped가 true일 때만 */}
                    {item.is_equipped && (
                        <Badge className={styles.badgeEquipped}>장착중</Badge>
                    )}
                </div>
            </CardContent>
        </Card>
    );
}

// ────────────────────────────────────────────────────────────
// SkeletonCard 컴포넌트 — 로딩 중 표시용
//
// Skeleton: shadcn의 shimmer 애니메이션 회색 블록
// 실제 카드와 동일한 구조로 배치 → 로딩 후 레이아웃 점프 없음
// ────────────────────────────────────────────────────────────
function SkeletonCard() {
    return (
        <Card>
            <CardContent className={styles.skeletonContent}>
                {/* 이미지 영역 크기와 동일하게 */}
                <Skeleton className={styles.skeletonImage} />
                {/* 아이템 이름 자리 */}
                <Skeleton className={styles.skeletonText} />
                {/* 배지 자리 — 더 짧게 */}
                <Skeleton className={`${styles.skeletonText} ${styles.skeletonTextShort}`} />
            </CardContent>
        </Card>
    );
}

// ────────────────────────────────────────────────────────────
// AvatarPage — 메인 페이지 컴포넌트
// ────────────────────────────────────────────────────────────
export default function AvatarPage() {

    // ──────────────────────────────────────────────────────────
    // 훅에서 서버 데이터와 액션 전부 가져오기
    //
    // 페이지 컴포넌트는 "어떻게 데이터를 가져오는가"를 몰라도 됨
    // → useAvatarItems가 API 호출, 상태 관리, toast 처리를 모두 담당
    //
    // minting: NFT 민팅 진행 중 여부 — Dialog 버튼 비활성화에 사용
    // ──────────────────────────────────────────────────────────
    const { items, loading, error, refetch, mintNft, minting } = useAvatarItems();

    // ──────────────────────────────────────────────────────────
    // 로컬 UI 상태
    //
    // activeTab: 탭 필터 선택값 — 서버 데이터와 무관한 순수 UI 상태
    // selectedItem: Dialog에 표시할 아이템 — null이면 Dialog 닫힘
    //
    // 이 두 상태는 훅으로 분리하지 않음:
    //   → 이 컴포넌트에서만 쓰이는 UI 상태
    //   → 훅으로 빼면 오히려 흐름 파악이 어려워짐
    // ──────────────────────────────────────────────────────────
    const [activeTab, setActiveTab] = useState<TabValue>("all");
    const [selectedItem, setSelectedItem] = useState<UserItemResponse | null>(null);

    // ──────────────────────────────────────────────────────────
    // 탭 필터링
    //
    // useMemo 미사용 이유:
    //   - items 배열이 수백 개 이하면 매 렌더링에 filter해도 충분히 빠름
    //   - useMemo는 최적화 도구 — 먼저 측정하고 필요할 때 적용
    // ──────────────────────────────────────────────────────────
    const filteredItems =
        activeTab === "all"
            ? items
            : items.filter((item) => item.category === activeTab);

    // ──────────────────────────────────────────────────────────
    // NFT 민팅 핸들러
    //
    // selectedItem이 null이면 얼리 리턴 (타입 가드)
    // 실제 Solana 민팅은 추후 연결 — 지금은 placeholder mint address 사용
    // mintNft 완료 후 훅 내부에서 fetchItems()가 재호출되므로
    // 여기서는 Dialog만 닫으면 됨
    // ──────────────────────────────────────────────────────────
    const handleMintNft = async () => {
        if (!selectedItem) return;
        // TODO: 실제 Solana 온체인 민팅 후 생성된 mint address로 교체
        await mintNft(selectedItem.id, "11111111111111111111111111111111");
        setSelectedItem(null); // 성공 시 Dialog 닫기
    };

    return (
        <div className={styles.pageWrapper}>
            <h1 className={styles.pageTitle}>내 아바타 아이템</h1>
            <p className={styles.pageSubtitle}>보유한 아이템을 확인하고 NFT로 민팅하세요</p>

            {/* ── 카테고리 탭 필터 ── */}
            {/*
        Tabs: shadcn의 탭 컴포넌트
        value/onValueChange: 제어 컴포넌트 패턴 (controlled)
        v as TabValue: onValueChange는 string을 주는데 우리 타입으로 단언
      */}
            <div className={styles.tabsWrapper}>
                <Tabs
                    value={activeTab}
                    onValueChange={(v) => setActiveTab(v as TabValue)}
                >
                    <TabsList>
                        <TabsTrigger value="all">전체</TabsTrigger>
                        <TabsTrigger value="background">배경</TabsTrigger>
                        <TabsTrigger value="frame">프레임</TabsTrigger>
                        <TabsTrigger value="effect">효과</TabsTrigger>
                        <TabsTrigger value="motion">모션</TabsTrigger>
                    </TabsList>
                </Tabs>
            </div>

            {/* ── 에러 상태 ── */}
            {/*
        에러가 있을 때만 렌더링
        refetch: 훅에서 노출한 수동 재조회 함수
        → "다시 시도" 버튼에서 직접 호출
      */}
            {error && (
                <div className={styles.errorState}>
                    <p className={styles.errorMessage}>{error}</p>
                    <Button variant="outline" onClick={refetch}>
                        다시 시도
                    </Button>
                </div>
            )}

            {/* ── 로딩 상태 ── */}
            {/*
        loading이 true이고 에러가 없을 때만 표시
        에러가 있는데 loading도 true인 경우는 에러 UI가 우선
        Array.from({ length: 8 }): 빈 배열 8개 생성 → Skeleton 8개 렌더링
        index를 key로 쓰는 것은 보통 비권장이지만,
        Skeleton은 순서가 바뀌지 않으므로 OK
      */}
            {loading && !error && (
                <div className={styles.grid}>
                    {Array.from({ length: 8 }).map((_, i) => (
                        <SkeletonCard key={i} />
                    ))}
                </div>
            )}

            {/* ── 정상 데이터 표시 ── */}
            {/*
        로딩도 아니고 에러도 아닐 때만 렌더링
        filteredItems가 비어있으면 빈 상태 메시지
      */}
            {!loading && !error && (
                <>
                    {filteredItems.length === 0 ? (
                        <div className={styles.emptyState}>
                            {activeTab === "all"
                                ? "보유한 아이템이 없습니다."
                                : "해당 카테고리의 아이템이 없습니다."}
                        </div>
                    ) : (
                        <div className={styles.grid}>
                            {filteredItems.map((item) => (
                                <ItemCard
                                    key={item.id}        // DB UUID를 key로 — 리스트 재정렬 시 안전
                                    item={item}
                                    onClick={() => setSelectedItem(item)} // 클릭 시 이 아이템을 선택
                                />
                            ))}
                        </div>
                    )}
                </>
            )}

            {/* ── 아이템 상세 Dialog ── */}
            {/*
        open={!!selectedItem}: selectedItem이 null이 아니면 true
        onOpenChange: ESC키 또는 바깥 클릭 시 Dialog 닫기
          → open이 false가 되면 selectedItem을 null로 초기화
      */}
            <Dialog
                open={!!selectedItem}
                onOpenChange={(open) => !open && setSelectedItem(null)}
            >
                {/*
          selectedItem && ...: Dialog가 열려있을 때만 내부 렌더링
          null일 때 렌더링하면 에러 발생하므로 조건부 렌더링 필수
        */}
                {selectedItem && (
                    <DialogContent>
                        <DialogHeader>
                            <DialogTitle>{selectedItem.name}</DialogTitle>
                            <DialogDescription>
                                {categoryLabel[selectedItem.category]} · {rarityLabel[selectedItem.rarity]}
                            </DialogDescription>
                        </DialogHeader>

                        {/* 아이템 이미지 */}
                        <div className={styles.dialogImageWrapper}>
                            {selectedItem.image_url ? (
                                <img
                                    src={selectedItem.image_url}
                                    alt={selectedItem.name}
                                    className={styles.dialogImage}
                                />
                            ) : (
                                <div className={styles.imagePlaceholder}>이미지 없음</div>
                            )}
                        </div>

                        {/* 상세 정보 테이블 */}
                        <div className={styles.detailTable}>
                            <div className={styles.detailRow}>
                                <span className={styles.detailLabel}>카테고리</span>
                                <span>{categoryLabel[selectedItem.category]}</span>
                            </div>

                            <div className={styles.detailRow}>
                                <span className={styles.detailLabel}>레어리티</span>
                                <Badge className={rarityClassMap[selectedItem.rarity]}>
                                    {rarityLabel[selectedItem.rarity]}
                                </Badge>
                            </div>

                            <div className={styles.detailRow}>
                                <span className={styles.detailLabel}>획득일</span>
                                <span>
                  {selectedItem.acquired_at
                      // ISO 8601 문자열 → 한국어 날짜 형식으로 변환
                      ? new Date(selectedItem.acquired_at).toLocaleDateString("ko-KR")
                      : "알 수 없음"}
                </span>
                            </div>

                            <div className={styles.detailRow}>
                                <span className={styles.detailLabel}>NFT 여부</span>
                                <span>{selectedItem.is_nft ? "✅ 민팅됨" : "❌ 미민팅"}</span>
                            </div>

                            <div className={styles.detailRow}>
                                <span className={styles.detailLabel}>장착 상태</span>
                                <span>{selectedItem.is_equipped ? "장착중" : "미장착"}</span>
                            </div>
                        </div>

                        {/*
              NFT 민팅 버튼 — is_nft가 false일 때만 렌더링
              이미 민팅된 아이템에는 버튼 자체를 숨김
              disabled={minting}: 민팅 중 중복 클릭 방지
            */}
                        {!selectedItem.is_nft && (
                            <Button
                                className={styles.mintButton}
                                onClick={handleMintNft}
                                disabled={minting}
                            >
                                {minting ? "민팅 중..." : "NFT 민팅"}
                            </Button>
                        )}
                    </DialogContent>
                )}
            </Dialog>
        </div>
    );
}