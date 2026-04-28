const anchor = require("@coral-xyz/anchor");
const { PublicKey, SystemProgram } = require("@solana/web3.js");

const TOKEN_PROGRAM_ID = new PublicKey(
  "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"
);
const ASSOCIATED_TOKEN_PROGRAM_ID = new PublicKey(
  "ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJe8bYh"
);

function deriveAta(owner, mint) {
  return PublicKey.findProgramAddressSync(
    [owner.toBuffer(), TOKEN_PROGRAM_ID.toBuffer(), mint.toBuffer()],
    ASSOCIATED_TOKEN_PROGRAM_ID
  )[0];
}

function parseAmountBaseUnits() {
  const amount = process.env.SPT_AMOUNT_BASE_UNITS;
  if (!amount || !/^\d+$/.test(amount)) {
    throw new Error("SPT_AMOUNT_BASE_UNITS 환경변수가 필요합니다.");
  }
  return new anchor.BN(amount);
}

async function main() {
  const provider = anchor.AnchorProvider.env();
  anchor.setProvider(provider);

  const program = anchor.workspace.Spentopia ?? anchor.workspace.spentopia;
  if (!program) {
    throw new Error("Anchor workspace에서 spentopia program을 찾을 수 없습니다.");
  }

  const user = new PublicKey(
    process.env.SPT_RECIPIENT_WALLET ?? provider.wallet.publicKey.toBase58()
  );
  const amount = parseAmountBaseUnits();

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
  const userTokenAccount = deriveAta(user, sptTokenMint);

  console.log("programId:", program.programId.toBase58());
  console.log("admin:", provider.wallet.publicKey.toBase58());
  console.log("recipient:", user.toBase58());
  console.log("sptTokenMint:", sptTokenMint.toBase58());
  console.log("userTokenAccount:", userTokenAccount.toBase58());
  console.log("amountBaseUnits:", amount.toString());

  const tx = await program.methods
    .mintSptToUser(amount)
    .accounts({
      platformConfig,
      admin: provider.wallet.publicKey,
      user,
      sptTokenMint,
      sptTokenAuthority,
      userTokenAccount,
      tokenProgram: TOKEN_PROGRAM_ID,
      associatedTokenProgram: ASSOCIATED_TOKEN_PROGRAM_ID,
      systemProgram: SystemProgram.programId,
    })
    .rpc();

  console.log("mint_spt_to_user tx:", tx);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
