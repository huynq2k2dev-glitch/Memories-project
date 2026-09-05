"use client";

import Link from "next/link";

import { useAuth } from "@/components/auth-provider";

export default function Home() {
  const { status } = useAuth();
  const primaryHref = status === "authenticated" ? "/memories/new" : "/register";
  const primaryLabel = status === "authenticated" ? "Tạo memory" : "Bắt đầu miễn phí";

  return (
    <main className="landing-page">
      <section className="landing-hero" aria-labelledby="platform-title">
        <div className="landing-copy">
          <p className="eyebrow">Gìn giữ điều đáng nhớ</p>
          <h1 id="platform-title">Mỗi kỷ niệm đều xứng đáng có một nơi để trở về.</h1>
          <p className="summary">
            Tạo một không gian riêng cho câu chuyện, hình ảnh và những lời chúc từ
            những người bạn yêu quý.
          </p>
          <div className="landing-actions">
            <Link className="primary-link" href={primaryHref}>
              {primaryLabel}
            </Link>
            <Link className="text-link" href="#how-it-works">
              Xem cách hoạt động
            </Link>
          </div>
        </div>
        <div className="memory-mockups" aria-label="Minh họa các mẫu memory">
          <article className="memory-mockup memory-mockup-main">
            <span>12 · 10 · 2026</span>
            <h2>Ngày mình về chung một nhà</h2>
            <p>Một hành trình nhỏ, được kể bằng những điều dịu dàng nhất.</p>
          </article>
          <article className="memory-mockup memory-mockup-note">
            <span>Nhật ký</span>
            <p>“Có những ngày bình thường, sau này lại thành điều ta nhớ nhất.”</p>
          </article>
        </div>
      </section>

      <section id="how-it-works" className="how-it-works" aria-labelledby="steps-title">
        <p className="eyebrow">Ba bước đơn giản</p>
        <h2 id="steps-title">Từ ý tưởng đến một memory của riêng bạn</h2>
        <div className="step-grid">
          <article>
            <span>01</span>
            <h3>Chọn dịp</h3>
            <p>Chọn loại memory phù hợp với câu chuyện bạn muốn kể.</p>
          </article>
          <article>
            <span>02</span>
            <h3>Chọn mẫu</h3>
            <p>Xem trước template và chọn phong cách gần với bạn nhất.</p>
          </article>
          <article>
            <span>03</span>
            <h3>Biên soạn</h3>
            <p>Đặt tên, thêm nội dung và hoàn thiện memory theo nhịp riêng.</p>
          </article>
        </div>
      </section>
    </main>
  );
}
