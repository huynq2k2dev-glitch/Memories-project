import LoginClient from "./login-client";

type LoginPageProps = {
  searchParams: Promise<{
    next?: string | string[];
    verified?: string | string[];
  }>;
};

export default async function LoginPage({ searchParams }: LoginPageProps) {
  const query = await searchParams;
  return (
    <LoginClient
      returnTo={typeof query.next === "string" ? query.next : undefined}
      verified={query.verified === "1"}
    />
  );
}
