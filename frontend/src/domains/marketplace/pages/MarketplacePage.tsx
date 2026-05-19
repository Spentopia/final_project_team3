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

import { useEffect, useState } from "react";
import { toast } from "sonner";
import { useConnection, useWallet } from "@solana/wallet-adapter-react";
import { useWalletModal } from "@solana/wallet-adapter-react-ui";

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
import { getOwnedNfts, syncOwnedNfts } from "@/domains/avatar/api/avatarApi";
import { abandonPendingListing } from "@/domains/marketplace/api/marketApi";
import { useMarket } from "../hooks/useMarket";

// 타입도 도메인 간 참조
import type { UserItemResponse } from "@/domains/avatar/model/types";
import type { ListingResponse } from "../model/types";
import { buyNftOnChain, cancelListingOnChain, listNftOnChain } from "../lib/marketplaceSolana";
import { useSptBalance } from "@/shared/hooks/useSptBalance";
import { getUserProfile } from "@/domains/profile/api/profile";

import styles from "./MarketplacePage.module.css";

// ────────────────────────────────────────────────────────────
// 카테고리 매핑 상수
// ────────────────────────────────────────────────────────────
const categoryLabel: Record<string, string> = {
  hair: "헤어",
  top: "상의",
  bottom: "하의",
  shoes: "신발",
  weapon: "무기",
  hat: "모자",
};

function getMarketplaceErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof Error && error.message.trim()) {
    return error.message;
  }
  if (typeof error === "string" && error.trim()) {
    return error;
  }
  return fallback;
}

function isWalletRejectionError(error: unknown): boolean {
  const message = error instanceof Error ? error.message : String(error ?? "");
  const normalized = message.toLowerCase();
  return (
    normalized.includes("user rejected") ||
    normalized.includes("rejected by user") ||
    normalized.includes("user denied") ||
    normalized.includes("transaction rejected") ||
    normalized.includes("요청을 거부")
  );
}

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
        <DialogContent className={styles.marketDialogContent}>
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
                          <Badge variant="outline">
                            {categoryLabel[item.category] ?? item.category}
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
  isOwn: boolean;           // 현재 로그인 유저가 판매자인지 여부
  onBuy: () => void;        // 부모에서 purchaseTarget 세팅 → AlertDialog 오픈
  onCancel: () => void;     // 부모에서 cancelTarget 세팅 → AlertDialog 오픈
}

function ListingCard({ listing, isOwn, onBuy, onCancel }: ListingCardProps) {
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

          {/* 카테고리 배지 */}
          <div className={styles.badgeRow}>
            <Badge variant="outline">
              {categoryLabel[listing.item_category] ?? listing.item_category}
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
          {isOwn ? (
            <Button
                className={styles.buyButton}
                size="sm"
                variant="outline"
                onClick={onCancel}
                disabled={!listing.escrow_address}
            >
              판매 취소
            </Button>
          ) : (
            <Button
                className={styles.buyButton}
                size="sm"
                onClick={onBuy}
                disabled={!listing.escrow_address}
            >
              {listing.escrow_address ? "구매" : "동기화 중"}
            </Button>
          )}
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
  const { connection } = useConnection();
  const { publicKey, connected, sendTransaction } = useWallet();
  const { setVisible: setWalletModalVisible } = useWalletModal();
  const { items, loading: itemsLoading, refetch: refetchItems } = useAvatarItems();
  const [walletAddress, setWalletAddress] = useState<string | null>(null);
  const { sptBalance, sptLoading, refreshSptBalance } = useSptBalance(walletAddress);

  useEffect(() => {
    getUserProfile()
      .then((p) => setWalletAddress(p.wallet_address))
      .catch(() => {});
  }, []);

  useEffect(() => {
    const handleWalletChange = (event: Event) => {
      const nextWalletAddress = (event as CustomEvent<{ walletAddress: string | null }>).detail
        ?.walletAddress;
      setWalletAddress(nextWalletAddress ?? null);
    };

    window.addEventListener("spentopia:wallet-change", handleWalletChange);
    return () => window.removeEventListener("spentopia:wallet-change", handleWalletChange);
  }, []);
  const {
    listings,
    listingsLoading,
    createListing,
    updateEscrow,
    purchaseItem,
    removeListingLocally,
    cancelListing,
    creatingListing,
    updatingEscrow,
    purchasing,
    cancelling,
  } = useMarket();

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
  const [cancelTarget, setCancelTarget] = useState<ListingResponse | null>(null);
  const [listingOnChain, setListingOnChain] = useState(false);
  const [cancellingOnChain, setCancellingOnChain] = useState(false);
  const [pendingAvailablePurchaseCount, setPendingAvailablePurchaseCount] = useState(0);
  const [ownedNftCount, setOwnedNftCount] = useState<number | null>(null);
  const [ownedNftLoading, setOwnedNftLoading] = useState(false);
  const syncingOwnedNfts = ownedNftLoading;
  const purchaseBalanceShortage = Boolean(
    purchaseTarget &&
      walletAddress &&
      !sptLoading &&
      (sptBalance ?? 0) < purchaseTarget.price_spt,
  );

  const refreshOwnedNftCount = async () => {
    setOwnedNftLoading(true);
    try {
      const ownedNfts = await getOwnedNfts();
      setOwnedNftCount(ownedNfts.length);
    } catch {
      setOwnedNftCount(null);
    } finally {
      setOwnedNftLoading(false);
    }
  };

  useEffect(() => {
    void refreshOwnedNftCount();
  }, [walletAddress]);

  const syncInventoryAfterMarketChange = async () => {
    // 구매/취소 API가 성공했다면 DB 상태는 이미 바뀌었으므로
    // 카운트 반영은 온체인 인덱싱을 기다리지 않고 먼저 즉시 당긴다.
    await refetchItems();
    await refreshOwnedNftCount();
    setPendingAvailablePurchaseCount(0);

    // 온체인 보정 동기화는 카운트 표시와 분리해서 한 번만 늦게 실행한다.
    // 짧은 연속 호출은 Helius/백엔드 rate limit을 건드릴 수 있다.
    window.setTimeout(() => {
      void syncOwnedNfts()
        .then(() => refetchItems())
        .catch(() => {});
    }, 5000);
  };

  // ──────────────────────────────────────────────────────────
  // NFT 아이템 필터링
  // is_nft === true인 아이템만 판매 등록 가능
  // is_nft가 boolean | null이므로 === true로 엄격하게 비교
  // ──────────────────────────────────────────────────────────
  const connectedWalletAddress = publicKey?.toBase58() ?? null;

  const ensureLinkedWalletConnected = (actionLabel: string) => {
    if (!walletAddress) {
      toast.error(`${actionLabel} 하려면 먼저 계정에 지갑을 연동해 주세요.`);
      return false;
    }

    if (!connected || !connectedWalletAddress) {
      setWalletModalVisible(true);
      toast.error(`${actionLabel} 하려면 연동된 지갑을 연결해 주세요.`);
      return false;
    }

    if (connectedWalletAddress !== walletAddress) {
      toast.error("현재 연결된 지갑이 계정에 연동된 지갑과 다릅니다. 연동된 지갑으로 다시 연결해 주세요.");
      return false;
    }

    return true;
  };

  // 이미 마켓에 등록된 내 NFT mint 주소 집합 (중복 등록 방지 + 카운트 제외용)
  const myListedMints = new Set(
    listings
      .filter((l) => walletAddress && l.seller_wallet_address === walletAddress)
      .map((l) => l.nft_mint_address)
      .filter(Boolean),
  );

  const nftItems = items.filter((item) => {
    if (item.is_nft !== true || !item.nft_mint_address) return false;
    if (myListedMints.has(item.nft_mint_address)) return false; // 이미 판매 등록됨
    return true;
  });

  // ──────────────────────────────────────────────────────────
  // 판매 등록 핸들러
  // createListing 내부에서 성공/실패 toast 처리
  // ──────────────────────────────────────────────────────────
  const handleCreateListing = async (itemId: string, priceSpt: number) => {
    if (!publicKey || !ensureLinkedWalletConnected("판매 등록을")) {
      return;
    }

    const item = nftItems.find((candidate) => candidate.id === itemId);
    if (!item?.nft_mint_address) {
      toast.error("NFT mint 주소가 없는 아이템은 판매 등록할 수 없습니다.");
      return;
    }

    setListingOnChain(true);
    let pendingListing: ListingResponse | null = null;
    try {
      const listing = await createListing(itemId, priceSpt);
      if (!listing) return;
      pendingListing = listing;

      toast.info("지갑에서 판매 등록을 승인해 주세요.");
      const { signature, escrowAddress } = await listNftOnChain({
        connection,
        publicKey,
        sendTransaction,
        nftMintAddress: item.nft_mint_address,
        priceSpt,
      });

      const escrowSaved = await updateEscrow(listing.id, escrowAddress, signature);
      if (!escrowSaved) {
        toast.error("온체인 판매 등록은 완료됐지만 DB 에스크로 저장에 실패했습니다. 다시 동기화가 필요합니다.");
      } else {
        setOwnedNftCount((count) => (count === null ? null : Math.max(count - 1, 0)));
      }
      void refetchItems();
    } catch (error) {
      if (pendingListing && isWalletRejectionError(error)) {
        try {
          await abandonPendingListing(pendingListing.id);
          await syncInventoryAfterMarketChange();
          toast.info("판매 등록이 취소되었습니다. NFT는 등록 가능 목록에 다시 반영됩니다.");
        } catch (cleanupError) {
          await refetchItems();
          await refreshOwnedNftCount();
          console.error("[abandonPendingListing]", cleanupError);
          toast.error("판매 등록 취소 처리에 실패했습니다. 관리자에게 문의해 주세요.");
        }
        return;
      }

      toast.error(getMarketplaceErrorMessage(error, "온체인 판매 등록 중 오류가 발생했습니다."));
    } finally {
      setListingOnChain(false);
    }
  };

  // ──────────────────────────────────────────────────────────
  // 구매 확인 핸들러
  // AlertDialog의 "구매 확인" 버튼 클릭 시 호출
  //
  // ──────────────────────────────────────────────────────────
  const handleConfirmPurchase = async () => {
    if (!purchaseTarget) return; // 타입 가드

    if (!publicKey || !ensureLinkedWalletConnected("구매를")) {
      return;
    }
    if (purchaseTarget.seller_wallet_address === publicKey.toBase58()) {
      toast.error("본인 상품은 구매할 수 없습니다.");
      return;
    }
    if (!purchaseTarget.seller_wallet_address || !purchaseTarget.nft_mint_address) {
      toast.error("구매에 필요한 온체인 주소 정보가 없습니다.");
      return;
    }
    if (!purchaseTarget.escrow_address) {
      toast.error("온체인 에스크로가 연결되지 않은 리스팅입니다.");
      return;
    }

    try {
      const signature = await buyNftOnChain({
        connection,
        publicKey,
        sendTransaction,
        sellerWalletAddress: purchaseTarget.seller_wallet_address,
        nftMintAddress: purchaseTarget.nft_mint_address,
        priceSpt: purchaseTarget.price_spt,
      });
      removeListingLocally(purchaseTarget.id);
      setPendingAvailablePurchaseCount((count) => count + 1);
      setPurchaseTarget(null);
      refreshSptBalance();

      const result = await purchaseItem(purchaseTarget.id, signature, {
        suppressErrorToast: true,
      });
      if (result) {
        await syncInventoryAfterMarketChange();
        return;
      }
      toast.error("온체인 구매는 완료됐지만 서버 거래 동기화에 실패했습니다. 판매자 알림도 생성되지 않을 수 있어 다시 확인이 필요합니다.");
    } catch (error) {
      setPurchaseTarget(null);
      toast.error(getMarketplaceErrorMessage(error, "온체인 구매 중 오류가 발생했습니다."));
    }
  };

  const handleConfirmCancel = async () => {
    if (!cancelTarget) return;
    if (!cancelTarget.nft_mint_address) {
      toast.error("NFT mint 주소가 없습니다.");
      return;
    }
    if (!ensureLinkedWalletConnected("판매 취소를")) {
      return;
    }

    setCancellingOnChain(true);
    try {
      if (!publicKey) return;
      const signature = await cancelListingOnChain({
        connection,
        publicKey,
        sendTransaction,
        nftMintAddress: cancelTarget.nft_mint_address,
      });
      const ok = await cancelListing(cancelTarget.id, signature);
      if (ok) {
        setCancelTarget(null);
        await syncInventoryAfterMarketChange();
      }
    } catch (error) {
      console.error("[cancelListing]", error);
      toast.error(getMarketplaceErrorMessage(error, "온체인 판매 취소 중 오류가 발생했습니다."));
    } finally {
      setCancellingOnChain(false);
    }
  };

  return (
      <div className={styles.pageWrapper}>

        {/* ── 페이지 헤더 ── */}
        <div className={styles.pageHeader}>
          <div className={styles.headerCopy}>
            <span className={styles.pageEyebrow}>NFT MARKET</span>
            <h1 className={styles.pageTitle}>마켓플레이스</h1>
            <p className={styles.pageSubtitle}>
              보유한 NFT 아이템을 SPT로 거래하고 새로운 아바타 파츠를 확인하세요.
            </p>
          </div>

          <div className={styles.headerActions}>
            <div className={styles.summaryStrip}>
              <div className={styles.summaryItem}>
                <span className={styles.summaryLabel}>판매중</span>
                <strong>{listings.length.toLocaleString()}</strong>
              </div>
              <div className={styles.summaryItem}>
                <span className={styles.summaryLabel}>등록 가능</span>
                <strong>{((ownedNftCount ?? nftItems.length) + pendingAvailablePurchaseCount).toLocaleString()}</strong>
              </div>
              <div className={styles.summaryItem}>
                <span className={styles.summaryLabel}>내 SPT</span>
                <strong>
                  {!walletAddress
                    ? "—"
                    : sptLoading
                    ? "..."
                    : (sptBalance ?? 0).toLocaleString()}
                </strong>
              </div>
            </div>

            {/* 판매 등록 버튼 → Dialog 열기 */}
            <Button className={styles.createButton} onClick={() => setIsCreateDialogOpen(true)}>
              판매 등록
            </Button>
          </div>
        </div>

        {/* ── 판매 목록 섹션 ── */}
        <section className={styles.marketSection}>
          <div className={styles.sectionHeader}>
            <div>
              <h2 className={styles.sectionTitle}>판매 중인 아이템</h2>
              <p className={styles.sectionSubtitle}>최근 등록된 아이템을 카드 단위로 확인할 수 있습니다.</p>
            </div>
            <span className={styles.sectionCount}>{listings.length.toLocaleString()} listings</span>
          </div>

          {listingsLoading ? (
              <div className={styles.emptyState}>
                <p>판매 목록을 불러오는 중...</p>
              </div>
          ) : listings.length === 0 ? (
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
                        isOwn={
                          !!(walletAddress && listing.seller_wallet_address === walletAddress)
                        }
                        onBuy={() => setPurchaseTarget(listing)}
                        onCancel={() => setCancelTarget(listing)}
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
            itemsLoading={itemsLoading || syncingOwnedNfts} // 아이템/NFT 동기화 중이면 다이얼로그 내 로딩 표시
            onSubmit={handleCreateListing}
            submitting={creatingListing || updatingEscrow || listingOnChain} // 등록 중이면 버튼 비활성화
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
          <AlertDialogContent className={styles.marketDialogContent}>
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
                        수수료는 온체인 설정 기준으로 구매 시 계산됩니다.
                  </span>
                      {walletAddress && !sptLoading && (
                        <span className={styles.purchaseFeeNote}>
                          현재 보유 SPT: {(sptBalance ?? 0).toLocaleString()} SPT
                        </span>
                      )}
                      {purchaseBalanceShortage && (
                        <span className={styles.purchaseFeeNote}>
                          SPT가 부족합니다. SOL로 SPT를 구매하거나 보상으로 SPT 토큰을 획득한 뒤 다시 시도해 주세요.
                        </span>
                      )}
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
                  onClick={(event) => {
                    event.preventDefault();
                    void handleConfirmPurchase();
                  }}
                  className={styles.purchaseConfirmButton}
                  disabled={purchasing || sptLoading || purchaseBalanceShortage}
              >
                {purchasing ? "구매 중..." : "구매 확인"}
              </AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
        {/* ── 판매 취소 확인 AlertDialog ── */}
        <AlertDialog
            open={!!cancelTarget}
            onOpenChange={(open) => !open && setCancelTarget(null)}
        >
          <AlertDialogContent className={styles.marketDialogContent}>
            <AlertDialogHeader>
              <AlertDialogTitle>판매 취소 확인</AlertDialogTitle>
              <AlertDialogDescription>
                {cancelTarget && (
                    <>
                      <span className={styles.purchaseItemName}>{cancelTarget.item_name}</span>
                      {" "}판매를 취소하시겠습니까?
                      <span className={styles.purchaseFeeNote}>
                        NFT가 에스크로에서 회수되어 본인 지갑으로 반환됩니다.
                      </span>
                    </>
                )}
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel disabled={cancellingOnChain || cancelling}>취소</AlertDialogCancel>
              <AlertDialogAction
                  onClick={(e) => { e.preventDefault(); void handleConfirmCancel(); }}
                  disabled={cancellingOnChain || cancelling}
              >
                {cancellingOnChain || cancelling ? "처리 중..." : "판매 취소 확인"}
              </AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
      </div>
  );
}
