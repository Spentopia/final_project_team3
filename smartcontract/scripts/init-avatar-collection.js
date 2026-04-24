const anchor = require("@coral-xyz/anchor");
const { PublicKey, SystemProgram, SYSVAR_RENT_PUBKEY } = require("@solana/web3.js");

const TOKEN_METADATA_PROGRAM_ID = new PublicKey(
  "metaqbxxUerdq28cj1RbAWkYQm3ybzjb6a8bt518x1s"
);
const TOKEN_PROGRAM_ID = new PublicKey(
  "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"
);
const ASSOCIATED_TOKEN_PROGRAM_ID = new PublicKey(
  "ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJe8bYh"
);
const SYSVAR_INSTRUCTIONS_PUBKEY = new PublicKey(
  "Sysvar1nstructions1111111111111111111111111"
);

function deriveAta(owner, mint) {
  return PublicKey.findProgramAddressSync(
    [owner.toBuffer(), TOKEN_PROGRAM_ID.toBuffer(), mint.toBuffer()],
    ASSOCIATED_TOKEN_PROGRAM_ID
  )[0];
}

async function main() {
  const provider = anchor.AnchorProvider.env();
  anchor.setProvider(provider);

  const program = anchor.workspace.Spentopia ?? anchor.workspace.spentopia;
  if (!program) {
    throw new Error("Anchor workspace에서 spentopia program을 찾을 수 없습니다.");
  }

  const name =
    process.env.AVATAR_COLLECTION_NAME ?? "Spentopia Avatar Collection";
  const symbol = process.env.AVATAR_COLLECTION_SYMBOL ?? "SPTAV";
  const uri = process.env.AVATAR_COLLECTION_URI;

  if (!uri) {
    throw new Error("AVATAR_COLLECTION_URI 환경변수가 필요합니다.");
  }

  const [platformConfig] = PublicKey.findProgramAddressSync(
    [Buffer.from("platform_config")],
    program.programId
  );
  const [collectionMint] = PublicKey.findProgramAddressSync(
    [Buffer.from("avatar_collection_mint")],
    program.programId
  );
  const [sptTokenAuthority] = PublicKey.findProgramAddressSync(
    [Buffer.from("spt_token_authority")],
    program.programId
  );
  const [collectionMetadata] = PublicKey.findProgramAddressSync(
    [
      Buffer.from("metadata"),
      TOKEN_METADATA_PROGRAM_ID.toBuffer(),
      collectionMint.toBuffer(),
    ],
    TOKEN_METADATA_PROGRAM_ID
  );
  const [collectionMasterEdition] = PublicKey.findProgramAddressSync(
    [
      Buffer.from("metadata"),
      TOKEN_METADATA_PROGRAM_ID.toBuffer(),
      collectionMint.toBuffer(),
      Buffer.from("edition"),
    ],
    TOKEN_METADATA_PROGRAM_ID
  );
  const adminCollectionTokenAccount = deriveAta(
    provider.wallet.publicKey,
    collectionMint
  );

  console.log("programId:", program.programId.toBase58());
  console.log("platformConfig:", platformConfig.toBase58());
  console.log("collectionMint:", collectionMint.toBase58());
  console.log("collectionMetadata:", collectionMetadata.toBase58());
  console.log("collectionMasterEdition:", collectionMasterEdition.toBase58());

  const tx = await program.methods
    .initAvatarCollection(name, symbol, uri)
    .accounts({
      platformConfig,
      admin: provider.wallet.publicKey,
      collectionMint,
      collectionMetadata,
      collectionMasterEdition,
      sptTokenAuthority,
      adminCollectionTokenAccount,
      metadataProgram: TOKEN_METADATA_PROGRAM_ID,
      sysvarInstructions: SYSVAR_INSTRUCTIONS_PUBKEY,
      rent: SYSVAR_RENT_PUBKEY,
      tokenProgram: TOKEN_PROGRAM_ID,
      associatedTokenProgram: ASSOCIATED_TOKEN_PROGRAM_ID,
      systemProgram: SystemProgram.programId,
    })
    .rpc();

  console.log("init_avatar_collection tx:", tx);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
