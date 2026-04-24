import type { Metadata } from 'next'
import { Space_Grotesk, Montserrat } from 'next/font/google'
import './globals.css'
import Header from '@/components/Header'
import Sidebar from '@/components/Sidebar'
import AuthInitializer from '@/components/AuthInitializer'

const spaceGrotesk = Space_Grotesk({
  subsets: ['latin'],
  weight: ['300', '400', '500', '600', '700'],
  variable: '--font-space-grotesk',
})

const montserrat = Montserrat({
  subsets: ['latin'],
  weight: ['300', '400', '500', '600', '700'],
  variable: '--font-montserrat',
})

export const metadata: Metadata = {
  title: 'rawr',
  description:
    'This magazine is built on the taste of one man. It may be subjective and biased, and that is not denied.',
  openGraph: {
    title: 'rawr',
    description: 'A magazine built on the taste of one man.',
    type: 'website',
  },
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" className={`${spaceGrotesk.variable} ${montserrat.variable}`}>
      <body className="bg-black text-white font-sans min-h-screen">
        <AuthInitializer />
        <Header />
        <Sidebar />
        <main>{children}</main>
      </body>
    </html>
  )
}
