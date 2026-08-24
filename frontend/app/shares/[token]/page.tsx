import ShareLinkRedeemer from "./share-link-redeemer";

export default async function ShareLinkPage({
  params,
}: {
  params: Promise<{ token: string }>;
}) {
  const { token } = await params;
  return <ShareLinkRedeemer accessToken={token} />;
}
