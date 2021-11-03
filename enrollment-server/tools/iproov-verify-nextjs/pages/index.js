import dynamic from 'next/dynamic'
import Head from 'next/head'

const iProovMe = dynamic(
    () => import('@iproov/web'),
    {ssr: false}
)

export default function Home({ token }) {
    return (
        <div>
            <Head>
                <title>iProov verification demo</title>
                <meta name="description" content="iProov verification demo"/>
                <script src="https://cdn.jsdelivr.net/npm/@iproov/web"/>
            </Head>

            <iproov-me
                token={token}
                base_url="https://eu.rp.secure.iproov.me"
                debug="false"/>

        </div>
    )
}

Home.getInitialProps = async (ctx) => {
    const token = ctx.query.verifyToken;
    return { token: token }
}
