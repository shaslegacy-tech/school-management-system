import type { Metadata } from "next";
import { Manrope, Geist_Mono } from "next/font/google";
import { Providers } from "@/app/providers";
import "./globals.css";

const manrope = Manrope({
  variable: "--font-manrope",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "Little Home School | Campus",
  description: "School operations and learning, thoughtfully connected.",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="en" className={`${manrope.variable} ${geistMono.variable} h-full antialiased`}>
      <body className="min-h-full"><Providers>{children}</Providers></body>
    </html>
  );
}
