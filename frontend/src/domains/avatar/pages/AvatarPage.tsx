import { useState, useEffect, useCallback } from "react";

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

import { getUserItems, getOwnedNfts } from "../api/avatarApi";
import type { UserItemResponse, OwnedNftResponse } from "../model/types";
import styles from "./AvatarPage.module.css";

const SLOT_LABELS: Record<string, string> = {
    hair: "헤어",
    top: "상의",
    bottom: "하의",
    shoes: "신발",
    weapon: "무기",
    hat: "모자",
};

function SkeletonCard() {
    return (
        <Card>
            <CardContent className={styles.skeletonContent}>
                <Skeleton className={styles.skeletonImage} />
                <Skeleton className={styles.skeletonText} />
                <Skeleton className={`${styles.skeletonText} ${styles.skeletonTextShort}`} />
            </CardContent>
        </Card>
    );
}

// ── 일반 아이템 카드 ──────────────────────────────────────────
interface NormalItemCardProps {
    item: UserItemResponse;
    onClick: () => void;
}

function NormalItemCard({ item, onClick }: NormalItemCardProps) {
    const cardClass = [
        styles.itemCard,
        item.is_equipped ? styles.itemCardEquipped : "",
    ].join(" ");

    return (
        <Card className={cardClass} onClick={onClick}>
            <CardContent className={styles.cardContent}>
                <div className={styles.imageWrapper}>
                    {item.image_url ? (
                        <img src={item.image_url} alt={item.name} className={styles.itemImage} />
                    ) : (
                        <div className={styles.imagePlaceholderCommon}>🎨</div>
                    )}
                </div>
                <p className={styles.itemName}>{item.name}</p>
                <div className={styles.badgeRow}>
                    <Badge variant="outline">{SLOT_LABELS[item.category] ?? item.category}</Badge>
                    {item.is_equipped && <Badge className={styles.badgeEquipped}>장착중</Badge>}
                </div>
            </CardContent>
        </Card>
    );
}

// ── NFT 아이템 카드 ───────────────────────────────────────────
interface NftItemCardProps {
    nft: OwnedNftResponse;
    onClick: () => void;
}

function NftItemCard({ nft, onClick }: NftItemCardProps) {
    return (
        <Card className={`${styles.itemCard} ${styles.itemCardNft}`} onClick={onClick}>
            <CardContent className={styles.cardContent}>
                <div className={styles.imageWrapper}>
                    {nft.image_url ? (
                        <img src={nft.image_url} alt={nft.name} className={styles.itemImage} />
                    ) : (
                        <div className={styles.imagePlaceholderCommon}>✨</div>
                    )}
                    <span className={styles.nftOverlay}>NFT</span>
                </div>
                <p className={styles.itemName}>{nft.name}</p>
                <div className={styles.badgeRow}>
                    {nft.category && (
                        <Badge variant="outline">{SLOT_LABELS[nft.category] ?? nft.category}</Badge>
                    )}
                    <Badge className={styles.badgeNft}>NFT</Badge>
                </div>
            </CardContent>
        </Card>
    );
}

// ── 메인 페이지 ───────────────────────────────────────────────
type MainTab = "normal" | "nft";

export default function AvatarPage() {
    const [mainTab, setMainTab] = useState<MainTab>("normal");

    // 일반 아이템 상태
    const [normalItems, setNormalItems] = useState<UserItemResponse[]>([]);
    const [normalLoading, setNormalLoading] = useState(false);
    const [normalError, setNormalError] = useState<string | null>(null);

    // NFT 아이템 상태
    const [nftItems, setNftItems] = useState<OwnedNftResponse[]>([]);
    const [nftLoading, setNftLoading] = useState(false);
    const [nftError, setNftError] = useState<string | null>(null);
    const [nftInitialized, setNftInitialized] = useState(false);

    // 슬롯 필터
    const [slotFilter, setSlotFilter] = useState<string>("all");

    // 다이얼로그
    const [selectedNormal, setSelectedNormal] = useState<UserItemResponse | null>(null);
    const [selectedNft, setSelectedNft] = useState<OwnedNftResponse | null>(null);

    const fetchNormalItems = useCallback(async () => {
        setNormalLoading(true);
        setNormalError(null);
        try {
            const data = await getUserItems();
            setNormalItems(data.filter((item) => item.is_nft === false));
        } catch (err) {
            setNormalError(err instanceof Error ? err.message : "아이템 목록 조회 실패");
        } finally {
            setNormalLoading(false);
        }
    }, []);

    const fetchNftItems = useCallback(async () => {
        setNftLoading(true);
        setNftError(null);
        try {
            const data = await getOwnedNfts();
            setNftItems(data);
        } catch (err) {
            setNftError(err instanceof Error ? err.message : "NFT 목록 조회 실패");
        } finally {
            setNftLoading(false);
            setNftInitialized(true);
        }
    }, []);

    useEffect(() => {
        fetchNormalItems();
        fetchNftItems();
    }, [fetchNormalItems, fetchNftItems]);

    const filteredNormal =
        slotFilter === "all"
            ? normalItems
            : normalItems.filter((item) => item.category === slotFilter);

    const filteredNft =
        slotFilter === "all"
            ? nftItems
            : nftItems.filter((nft) => nft.category === slotFilter);

    const isLoading = mainTab === "normal" ? normalLoading : (nftLoading || !nftInitialized);
    const error = mainTab === "normal" ? normalError : nftError;
    const refetch = mainTab === "normal" ? fetchNormalItems : fetchNftItems;

    return (
        <div className={styles.pageWrapper}>
            <h1 className={styles.pageTitle}>내 아바타 아이템</h1>

            {/* ── 일반 / NFT 메인 탭 ── */}
            <div className={styles.tabsWrapper}>
                <Tabs value={mainTab} onValueChange={(v) => { setMainTab(v as MainTab); setSlotFilter("all"); }}>
                    <TabsList>
                        <TabsTrigger value="normal">일반 아이템</TabsTrigger>
                        <TabsTrigger value="nft">NFT 아이템</TabsTrigger>
                    </TabsList>
                </Tabs>
            </div>

            {/* ── 슬롯 필터 ── */}
            <div className={styles.tabsWrapper}>
                <Tabs value={slotFilter} onValueChange={setSlotFilter}>
                    <TabsList>
                        <TabsTrigger value="all">전체</TabsTrigger>
                        {Object.entries(SLOT_LABELS).map(([key, label]) => (
                            <TabsTrigger key={key} value={key}>{label}</TabsTrigger>
                        ))}
                    </TabsList>
                </Tabs>
            </div>

            {/* ── 에러 ── */}
            {error && (
                <div className={styles.errorState}>
                    <p className={styles.errorMessage}>{error}</p>
                    <Button variant="outline" onClick={refetch}>다시 시도</Button>
                </div>
            )}

            {/* ── 로딩 ── */}
            {isLoading && !error && (
                <div className={styles.grid}>
                    {Array.from({ length: 8 }).map((_, i) => <SkeletonCard key={i} />)}
                </div>
            )}

            {/* ── 일반 아이템 목록 ── */}
            {!isLoading && !error && mainTab === "normal" && (
                <>
                    {filteredNormal.length === 0 ? (
                        <div className={styles.emptyState}>보유한 아이템이 없습니다.</div>
                    ) : (
                        <div className={styles.grid}>
                            {filteredNormal.map((item) => (
                                <NormalItemCard key={item.id} item={item} onClick={() => setSelectedNormal(item)} />
                            ))}
                        </div>
                    )}
                </>
            )}

            {/* ── NFT 아이템 목록 ── */}
            {!isLoading && !error && mainTab === "nft" && (
                <>
                    {filteredNft.length === 0 ? (
                        <div className={styles.emptyState}>보유한 NFT 아이템이 없습니다.</div>
                    ) : (
                        <div className={styles.grid}>
                            {filteredNft.map((nft) => (
                                <NftItemCard key={nft.mint_address} nft={nft} onClick={() => setSelectedNft(nft)} />
                            ))}
                        </div>
                    )}
                </>
            )}

            {/* ── 일반 아이템 상세 Dialog ── */}
            <Dialog open={!!selectedNormal} onOpenChange={(open) => !open && setSelectedNormal(null)}>
                {selectedNormal && (
                    <DialogContent>
                        <DialogHeader>
                            <DialogTitle>{selectedNormal.name}</DialogTitle>
                            <DialogDescription>
                                {SLOT_LABELS[selectedNormal.category] ?? selectedNormal.category}
                            </DialogDescription>
                        </DialogHeader>
                        <div className={styles.dialogImageWrapper}>
                            {selectedNormal.image_url ? (
                                <img src={selectedNormal.image_url} alt={selectedNormal.name} className={styles.dialogImage} />
                            ) : (
                                <div className={styles.imagePlaceholder}>이미지 없음</div>
                            )}
                        </div>
                        <div className={styles.detailTable}>
                            <div className={styles.detailRow}>
                                <span className={styles.detailLabel}>슬롯</span>
                                <span>{SLOT_LABELS[selectedNormal.category] ?? selectedNormal.category}</span>
                            </div>
                            <div className={styles.detailRow}>
                                <span className={styles.detailLabel}>획득일</span>
                                <span>{selectedNormal.acquired_at ? new Date(selectedNormal.acquired_at).toLocaleDateString("ko-KR") : "알 수 없음"}</span>
                            </div>
                            <div className={styles.detailRow}>
                                <span className={styles.detailLabel}>장착 상태</span>
                                <span>{selectedNormal.is_equipped ? "장착중" : "미장착"}</span>
                            </div>
                        </div>
                    </DialogContent>
                )}
            </Dialog>

            {/* ── NFT 아이템 상세 Dialog ── */}
            <Dialog open={!!selectedNft} onOpenChange={(open) => !open && setSelectedNft(null)}>
                {selectedNft && (
                    <DialogContent>
                        <DialogHeader>
                            <DialogTitle>{selectedNft.name}</DialogTitle>
                            <DialogDescription>
                                {selectedNft.category ? (SLOT_LABELS[selectedNft.category] ?? selectedNft.category) : ""}
                            </DialogDescription>
                        </DialogHeader>
                        <div className={styles.dialogImageWrapper}>
                            {selectedNft.image_url ? (
                                <img src={selectedNft.image_url} alt={selectedNft.name} className={styles.dialogImage} />
                            ) : (
                                <div className={styles.imagePlaceholder}>이미지 없음</div>
                            )}
                        </div>
                        <div className={styles.detailTable}>
                            <div className={styles.detailRow}>
                                <span className={styles.detailLabel}>Mint 주소</span>
                                <span className="font-mono text-xs truncate">{selectedNft.mint_address}</span>
                            </div>
                            {selectedNft.metadata_uri && (
                                <div className={styles.detailRow}>
                                    <span className={styles.detailLabel}>Metadata</span>
                                    <a href={selectedNft.metadata_uri} target="_blank" rel="noopener noreferrer" className="text-xs text-blue-500 underline truncate">
                                        Pinata 링크
                                    </a>
                                </div>
                            )}
                        </div>
                    </DialogContent>
                )}
            </Dialog>
        </div>
    );
}
