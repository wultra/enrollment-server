import dynamic from 'next/dynamic'
import Head from 'next/head'

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
                <script src="https://cdn.jsdelivr.net/npm/@iproov/web"/>
            </Head>

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
