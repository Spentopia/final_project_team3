const anchor = require("@coral-xyz/anchor");
const { PublicKey, SystemProgram } = require("@solana/web3.js");

function parseU64(value, fallback) {
  const raw = value ?? fallback;
  if (!raw || !/^\d+$/.test(raw)) {
    throw new Error(`u64 숫자 문자열이 필요합니다: ${raw}`);
  }
  return new anchor.BN(raw);
}

async function main() {
  const provider = anchor.AnchorProvider.env();
  anchor.setProvider(provider);

  const program = anchor.workspace.Spentopia ?? anchor.workspace.spentopia;
  if (!program) {
    throw new Error("Anchor workspace에서 spentopia program을 찾을 수 없습니다.");
  }

  const feeRate = Number(process.env.PLATFORM_FEE_BPS ?? "500");
  if (!Number.isInteger(feeRate) || feeRate < 0 || feeRate > 10000) {
    throw new Error("PLATFORM_FEE_BPS는 0~10000 정수여야 합니다.");
  }

  const maxSupply = parseU64(
    process.env.SPT_MAX_SUPPLY_BASE_UNITS,
    "100000000000000"
  );

  const [platformConfig] = PublicKey.findProgramAddressSync(
    [Buffer.from("platform_config")],
    program.programId
  );
  const [sptTokenMint] = PublicKey.findProgramAddressSync(
    [Buffer.from("spt_token_mint")],
    program.programId
  );
  const [sptTokenAuthority] = PublicKey.findProgramAddressSync(
    [Buffer.from("spt_token_authority")],
    program.programId
  );

  console.log("programId:", program.programId.toBase58());
  console.log("admin:", provider.wallet.publicKey.toBase58());
  console.log("platformConfig:", platformConfig.toBase58());
  console.log("sptTokenMint:", sptTokenMint.toBase58());
  console.log("sptTokenAuthority:", sptTokenAuthority.toBase58());
  console.log("feeRateBps:", feeRate);
  console.log("maxSupplyBaseUnits:", maxSupply.toString());

  const tx = await program.methods
    .initPlatform(feeRate, maxSupply)
    .accounts({
      admin: provider.wallet.publicKey,
      platformConfig,
      sptTokenMint,
      sptTokenAuthority,
      systemProgram: SystemProgram.programId,
    })
    .rpc();

  console.log("init_platform tx:", tx);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
