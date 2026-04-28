const anchor = require("@coral-xyz/anchor");
const { PublicKey, SystemProgram } = require("@solana/web3.js");

const TOKEN_METADATA_PROGRAM_ID = new PublicKey(
  "metaqbxxUerdq28cj1RbAWkYQm3ybzjb6a8bt518x1s"
);
const TOKEN_PROGRAM_ID = new PublicKey(
  "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"
);
const SYSVAR_INSTRUCTIONS_PUBKEY = new PublicKey(
  "Sysvar1nstructions1111111111111111111111111"
);

async function main() {
  const provider = anchor.AnchorProvider.env();
  anchor.setProvider(provider);

  const program = anchor.workspace.Spentopia ?? anchor.workspace.spentopia;
  if (!program) {
    throw new Error("Anchor workspace에서 spentopia program을 찾을 수 없습니다.");
  }

  const name = process.env.SPT_TOKEN_NAME ?? "Spentopia Token";
  const symbol = process.env.SPT_TOKEN_SYMBOL ?? "SPT";
  const uri = process.env.SPT_TOKEN_URI;
  if (!uri) {
    throw new Error("SPT_TOKEN_URI 환경변수가 필요합니다.");
  }

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
  const [metadata] = PublicKey.findProgramAddressSync(
    [
      Buffer.from("metadata"),
      TOKEN_METADATA_PROGRAM_ID.toBuffer(),
      sptTokenMint.toBuffer(),
    ],
    TOKEN_METADATA_PROGRAM_ID
  );

  console.log("programId:", program.programId.toBase58());
  console.log("admin:", provider.wallet.publicKey.toBase58());
  console.log("platformConfig:", platformConfig.toBase58());
  console.log("sptTokenMint:", sptTokenMint.toBase58());
  console.log("sptTokenAuthority:", sptTokenAuthority.toBase58());
  console.log("metadata:", metadata.toBase58());

  const tx = await program.methods
    .initSptToken(name, symbol, uri)
    .accounts({
      platformConfig,
      admin: provider.wallet.publicKey,
      sptTokenMint,
      metadata,
      sptTokenAuthority,
      metadataProgram: TOKEN_METADATA_PROGRAM_ID,
      sysvarInstructions: SYSVAR_INSTRUCTIONS_PUBKEY,
      tokenProgram: TOKEN_PROGRAM_ID,
      systemProgram: SystemProgram.programId,
    })
    .rpc();

  console.log("init_spt_token tx:", tx);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
