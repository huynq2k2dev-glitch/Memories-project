import MemoryEditClient from "./memory-edit-client";

type MemoryEditPageProps = {
  params: Promise<{ slug: string }>;
};

export default async function MemoryEditPage({ params }: MemoryEditPageProps) {
  const { slug: memoryId } = await params;
  return <MemoryEditClient memoryId={memoryId} />;
}
