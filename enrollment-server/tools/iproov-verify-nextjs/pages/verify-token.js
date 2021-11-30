import dynamic from 'next/dynamic'
import Head from 'next/head'
import Script from 'next/script'

// Import the library in browser
const iProovMe = dynamic(
    () => import('@iproov/web'),
    {ssr: false}
)

export default function VerifyToken({ token }) {
    return (
        <div>
            <Head>
                <title>iProov demo - token verification</title>
                <meta name="description" content="iProov demo - token verification"/>
            </Head>
            <Script src="https://cdn.jsdelivr.net/npm/@iproov/web" strategy="beforeInteractive"/>

            <iproov-me
                token={token}
                base_url="https://eu.rp.secure.iproov.me"
                debug="false"/>

        </div>
    )
}

VerifyToken.getInitialProps = async (ctx) => {
    const token = ctx.query.value;
    return { token: token }
}
