// ============================================================
// MarketplacePage.tsx — 마켓플레이스 페이지
//
// 두 훅을 조합:
//   - useAvatarItems: 판매 등록 시 내 NFT 아이템 목록 선택용
//   - useMarket: 판매 등록 / 구매 액션 + 로컬 listings 상태
//
// 훅 재사용 포인트:
//   - useAvatarItems를 AvatarPage와 이 페이지 둘 다 사용
//   - 각 컴포넌트가 독립적인 훅 인스턴스를 가짐 (상태 공유 X)
//   - 상태 공유가 필요해지면 Context 또는 전역 상태로 이관
//
// CSS: MarketplacePage.module.css 로 전부 분리
// import 경로: @/shared/ui/...
// ============================================================

import { useState } from "react";

// shadcn/ui 컴포넌트 — 프로젝트 실제 경로 @/shared/ui
import { Card, CardContent, CardFooter } from "@/shared/ui/card";
import { Badge } from "@/shared/ui/badge";
import { Button } from "@/shared/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/shared/ui/dialog";
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogCancel,
  AlertDialogAction,
} from "@/shared/ui/alert-dialog";
// AlertDialog vs Dialog 구분:
//   - Dialog: 일반적인 모달 (폼, 상세 보기 등)
//   - AlertDialog: 파괴적/되돌릴 수 없는 액션 전 사용자 의도 재확인용
//     → 구매, 삭제 등에 사용
import { Separator } from "@/shared/ui/separator";

// 도메인 간 훅 참조 — 상대경로(..) 대신 절대경로(@/)로 명확하게
import { useAvatarItems } from "@/domains/avatar/hooks/useAvatarItems";
import { useMarket } from "../hooks/useMarket";

// 타입도 도메인 간 참조
import type { UserItemResponse } from "@/domains/avatar/model/types";
import type { ListingResponse } from "../model/types";

import styles from "./MarketplacePage.module.css";

// ────────────────────────────────────────────────────────────
// 레어리티 / 카테고리 매핑 상수
// ────────────────────────────────────────────────────────────
const rarityClassMap: Record<UserItemResponse["rarity"], string> = {
  common: styles.rarityCommon,
  rare: styles.rarityRare,
  epic: styles.rarityEpic,
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
// CreateListingDialog — 판매 등록 다이얼로그
//
// MarketplacePage에서 분리한 이유:
//   - 다이얼로그 내부의 선택 상태(selectedItemId, price)를
//     페이지 컴포넌트와 분리해서 관리
//   - 다이얼로그가 닫히면 내부 상태가 자동 초기화
//   - 페이지 컴포넌트가 단순해짐
// ────────────────────────────────────────────────────────────
interface CreateListingDialogProps {
  open: boolean;
  onClose: () => void;
  nftItems: UserItemResponse[];  // is_nft === true 필터링된 목록 (부모에서 전달)
  itemsLoading: boolean;
  onSubmit: (itemId: string, priceSpt: number) => Promise<void>;
  submitting: boolean;           // 등록 API 호출 중 여부 → 버튼 비활성화
}

function CreateListingDialog({
                               open,
                               onClose,
                               nftItems,
                               itemsLoading,
                               onSubmit,
                               submitting,
                             }: CreateListingDialogProps) {

  // ──────────────────────────────────────────────────────────
  // 다이얼로그 내부 로컬 상태
  // 다이얼로그가 닫히면 handleClose에서 초기화 → 다음 열기 시 깨끗하게 시작
  // ──────────────────────────────────────────────────────────
  const [selectedItemId, setSelectedItemId] = useState<string>("");
  const [price, setPrice] = useState<string>("");
  // price를 number가 아닌 string으로 관리하는 이유:
  //   - input value는 항상 string
  //   - 빈 문자열("")과 0을 구분하기 위함 (number면 초기값 0이 입력된 것처럼 보임)

  const handleClose = () => {
    // 다이얼로그 닫을 때 내부 상태 초기화
    setSelectedItemId("");
    setPrice("");
    onClose();
  };

  const handleSubmit = async () => {
    if (!selectedItemId || !price) return; // 선택/입력 없으면 얼리 리턴

    const parsed = parseInt(price, 10);
    // parseInt 두 번째 인수 10: 10진수로 파싱 (기수 명시 필수)
    // isNaN: 숫자가 아닌 문자열 입력 방어
    if (isNaN(parsed) || parsed <= 0) return;

    await onSubmit(selectedItemId, parsed);
    // onSubmit 내부(useMarket)에서 toast 처리
    // 성공 시 handleClose는 부모에서 설정한 onClose가 Dialog를 닫음
    handleClose();
  };

  return (
      <Dialog open={open} onOpenChange={(isOpen) => !isOpen && handleClose()}>
        {/*
        onOpenChange: ESC 또는 바깥 클릭 시 isOpen=false로 호출됨
        → handleClose()로 내부 상태 초기화 + 부모 onClose 호출
      */}
        <DialogContent>
          <DialogHeader>
            <DialogTitle>판매 등록</DialogTitle>
            <DialogDescription>
              NFT 민팅된 아이템만 판매 등록이 가능합니다.
            </DialogDescription>
          </DialogHeader>

          {/* ── NFT 아이템 선택 목록 ── */}
          {/*
          max-height + overflow-y: auto → CSS에서 처리
          아이템이 많아도 스크롤로 탐색 가능
        */}
          <div className={styles.dialogItemList}>
            {itemsLoading ? (
                <p className={styles.dialogEmptyText}>아이템 목록 불러오는 중...</p>
            ) : nftItems.length === 0 ? (
                <p className={styles.dialogEmptyText}>판매 가능한 NFT 아이템이 없습니다.</p>
            ) : (
                nftItems.map((item) => {
                  const isSelected = selectedItemId === item.id;
                  return (
                      <div
                          key={item.id}
                          className={[
                            styles.selectableItem,
                            isSelected ? styles.selectableItemActive : "",
                          ].join(" ")}
                          onClick={() => setSelectedItemId(item.id)}
                      >
                        {/* 썸네일 */}
                        <div className={styles.selectableItemThumb}>
                          {item.image_url ? (
                              <img src={item.image_url} alt={item.name} />
                          ) : (
                              <div /> // 이미지 없으면 빈 div (CSS에서 bg-gray 처리)
                          )}
                        </div>

                        {/* 아이템 정보 */}
                        <div className={styles.selectableItemInfo}>
                          <p className={styles.selectableItemName}>{item.name}</p>
                          <Badge className={rarityClassMap[item.rarity]}>
                            {rarityLabel[item.rarity]}
                          </Badge>
                        </div>
                      </div>
                  );
                })
            )}
          </div>

          {/* 구분선 */}
          <Separator className={styles.dialogDivider} />

          {/* ── 판매가 입력 ── */}
          <div>
            <label className={styles.priceLabel}>판매가 (SPT)</label>
            <input
                type="number"
                min={1}    // HTML5 최소값 (UX 힌트) — 실제 검증은 handleSubmit에서
                step={1}   // 정수 단위 입력
                value={price}
                onChange={(e) => setPrice(e.target.value)}
                placeholder="판매가를 입력하세요"
                className={styles.priceInput}
            />
          </div>

          <DialogFooter className={styles.dialogFooter}>
            <Button variant="outline" onClick={handleClose}>
              취소
            </Button>
            <Button
                onClick={handleSubmit}
                // 아이템 미선택, 가격 미입력, 제출 중 세 가지 모두 비활성화
                disabled={!selectedItemId || !price || submitting}
            >
              {submitting ? "등록 중..." : "판매 등록"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
  );
}

// ────────────────────────────────────────────────────────────
// ListingCard — 판매 중인 아이템 카드
//
// 구매 버튼 클릭 시 부모에서 AlertDialog를 열도록 onBuy 콜백 사용
// 카드 자체는 AlertDialog를 모름 → 관심사 분리
// ────────────────────────────────────────────────────────────
interface ListingCardProps {
  listing: ListingResponse;
  onBuy: () => void; // 부모에서 purchaseTarget 세팅 → AlertDialog 오픈
}

function ListingCard({ listing, onBuy }: ListingCardProps) {
  return (
      <Card className={styles.listingCard}>
        <CardContent className={styles.cardContent}>

          {/* 아이템 이미지 */}
          <div className={styles.imageWrapper}>
            {listing.item_image_url ? (
                <img
                    src={listing.item_image_url}
                    alt={listing.item_name}
                    className={styles.itemImage}
                />
            ) : (
                <div className={styles.imagePlaceholder}>이미지 없음</div>
            )}
          </div>

          {/* 아이템 이름 */}
          <p className={styles.itemName}>{listing.item_name}</p>

          {/* 레어리티 + 카테고리 배지 */}
          <div className={styles.badgeRow}>
            <Badge className={rarityClassMap[listing.item_rarity]}>
              {rarityLabel[listing.item_rarity]}
            </Badge>
            <Badge variant="outline">
              {categoryLabel[listing.item_category]}
            </Badge>
          </div>

          {/* 판매가 */}
          {/* toLocaleString(): 숫자에 천 단위 콤마 추가 (예: 1,000) */}
          <p className={styles.itemPrice}>{listing.price_spt.toLocaleString()} SPT</p>

          {/* 판매자 닉네임 */}
          {/* ?? 연산자: null 또는 undefined일 때만 우측 값 사용 */}
          <p className={styles.sellerName}>
            판매자: {listing.seller_nickname ?? "알 수 없음"}
          </p>
        </CardContent>

        <CardFooter className={styles.cardFooter}>
          <Button className={styles.buyButton} size="sm" onClick={onBuy}>
            구매
          </Button>
        </CardFooter>
      </Card>
  );
}

// ────────────────────────────────────────────────────────────
// MarketplacePage — 메인 페이지 컴포넌트
// ────────────────────────────────────────────────────────────
export default function MarketplacePage() {

  // ──────────────────────────────────────────────────────────
  // 훅 조합
  //
  // useAvatarItems: 내 아이템 목록 — 판매 등록 다이얼로그에서 선택용
  //   → items, loading만 사용 (mintNft 등은 이 페이지에서 불필요)
  //
  // useMarket: 판매 목록 상태 + 판매 등록/구매 액션
  //   → listings는 로컬 상태 (GET API 미구현으로 서버에서 못 가져옴)
  // ──────────────────────────────────────────────────────────
  const { items, loading: itemsLoading } = useAvatarItems();
  const { listings, createListing, purchaseItem, creatingListing, purchasing } = useMarket();

  // ──────────────────────────────────────────────────────────
  // 로컬 UI 상태
  //
  // isCreateDialogOpen: 판매 등록 Dialog 표시 여부
  // purchaseTarget: 구매 확인 AlertDialog에 표시할 리스팅
  //   - null이면 AlertDialog 닫힘
  //   - 특정 리스팅이면 AlertDialog 열림 + 해당 리스팅 정보 표시
  // ──────────────────────────────────────────────────────────
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const [purchaseTarget, setPurchaseTarget] = useState<ListingResponse | null>(null);

  // ──────────────────────────────────────────────────────────
  // NFT 아이템 필터링
  // is_nft === true인 아이템만 판매 등록 가능
  // is_nft가 boolean | null이므로 === true로 엄격하게 비교
  // ──────────────────────────────────────────────────────────
  const nftItems = items.filter((item) => item.is_nft === true);

  // ──────────────────────────────────────────────────────────
  // 판매 등록 핸들러
  // createListing 내부에서 성공/실패 toast 처리
  // ──────────────────────────────────────────────────────────
  const handleCreateListing = async (itemId: string, priceSpt: number) => {
    await createListing(itemId, priceSpt);
    // Dialog 닫기는 CreateListingDialog 내부 handleClose에서 처리
  };

  // ──────────────────────────────────────────────────────────
  // 구매 확인 핸들러
  // AlertDialog의 "구매 확인" 버튼 클릭 시 호출
  //
  // TODO: 실제 Solana 구매 트랜잭션 완료 후 tx_signature로 교체
  // 현재: placeholder 서명 사용 (백엔드 tx 검증은 추후)
  // ──────────────────────────────────────────────────────────
  const handleConfirmPurchase = async () => {
    if (!purchaseTarget) return; // 타입 가드

    await purchaseItem(purchaseTarget.id, "mock_tx_signature_placeholder");
    // 성공 시 useMarket 내부에서 listings 목록에서 해당 항목 제거
    setPurchaseTarget(null); // AlertDialog 닫기
  };

  return (
      <div className={styles.pageWrapper}>

        {/* ── 페이지 헤더 ── */}
        <div className={styles.pageHeader}>
          <h1 className={styles.pageTitle}>마켓플레이스</h1>
          {/* 판매 등록 버튼 → Dialog 열기 */}
          <Button onClick={() => setIsCreateDialogOpen(true)}>판매 등록</Button>
        </div>

        {/* ── 판매 목록 섹션 ── */}
        <section>
          <h2 className={styles.sectionTitle}>판매 중인 아이템</h2>

          {listings.length === 0 ? (
              // 로컬 listings 배열이 비어있을 때
              // GET API 구현 전까지는 새로고침하면 항상 이 상태로 시작
              <div className={styles.emptyState}>
                <p>판매 중인 아이템이 없습니다.</p>
                <p className={styles.emptyStateSub}>판매 등록 후 여기에 표시됩니다.</p>
              </div>
          ) : (
              <div className={styles.grid}>
                {listings.map((listing) => (
                    <ListingCard
                        key={listing.id}
                        listing={listing}
                        // 구매 버튼 클릭 시 이 listing을 purchaseTarget으로 저장
                        // → AlertDialog가 열리고 해당 리스팅 정보 표시
                        onBuy={() => setPurchaseTarget(listing)}
                    />
                ))}
              </div>
          )}
        </section>

        {/* ── 판매 등록 Dialog ── */}
        <CreateListingDialog
            open={isCreateDialogOpen}
            onClose={() => setIsCreateDialogOpen(false)}
            nftItems={nftItems}         // 필터링된 NFT 아이템 목록 전달
            itemsLoading={itemsLoading} // 아이템 로딩 중이면 다이얼로그 내 로딩 표시
            onSubmit={handleCreateListing}
            submitting={creatingListing} // 등록 중이면 버튼 비활성화
        />

        {/* ── 구매 확인 AlertDialog ── */}
        {/*
        open={!!purchaseTarget}: purchaseTarget이 null이 아니면 true
        onOpenChange: ESC 또는 바깥 클릭 시 purchaseTarget 초기화
      */}
        <AlertDialog
            open={!!purchaseTarget}
            onOpenChange={(open) => !open && setPurchaseTarget(null)}
        >
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>구매 확인</AlertDialogTitle>
              <AlertDialogDescription>
                {/*
                purchaseTarget이 있을 때만 렌더링
                없으면 AlertDialog가 닫혀 있어서 보이지 않지만,
                타입 가드로 안전하게 처리
              */}
                {purchaseTarget && (
                    <>
                  <span className={styles.purchaseItemName}>
                    {purchaseTarget.item_name}
                  </span>
                      을(를){" "}
                      <span className={styles.purchaseItemName}>
                    {purchaseTarget.price_spt.toLocaleString()} SPT
                  </span>
                      에 구매하시겠습니까?
                      <span className={styles.purchaseFeeNote}>
                    {/* 수수료 5% 미리 계산해서 표시 — 백엔드도 동일하게 계산 */}
                        수수료: {Math.floor(purchaseTarget.price_spt * 0.05).toLocaleString()} SPT (5%)
                  </span>
                    </>
                )}
              </AlertDialogDescription>
            </AlertDialogHeader>

            <AlertDialogFooter>
              {/*
              AlertDialogCancel: 취소 버튼 — 클릭 시 AlertDialog 자동 닫힘
              구매 중에는 취소도 비활성화 (중간에 취소하면 상태 불일치 위험)
            */}
              <AlertDialogCancel disabled={purchasing}>취소</AlertDialogCancel>

              {/*
              AlertDialogAction: 확인 버튼
              onClick에서 e.preventDefault()가 필요한 경우:
                shadcn AlertDialogAction은 클릭 시 자동으로 Dialog를 닫으려 함
                → 비동기 처리 후 수동으로 setPurchaseTarget(null) 해주므로
                  여기서는 기본 동작 유지해도 무방
            */}
              <AlertDialogAction
                  onClick={handleConfirmPurchase}
                  disabled={purchasing}
              >
                {purchasing ? "구매 중..." : "구매 확인"}
              </AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
      </div>
  );
}