import Head from 'next/head'
import Image from "next/image";
import Link from "next/link";
import Script from "next/script";
import {useState} from 'react';

// Import the library in browser
export default function Home() {
    const [token, setToken] = useState("")

    const handleChange = event => {
        setToken(event.target.value);
    }

    return (
        <div>
            <Head>
                <title>Wultra Enrollment Server - iProov demo</title>
                <meta name="description" content="iProov demo"/>
                <link href="https://cdn.jsdelivr.net/npm/bootstrap@4.6.0/dist/css/bootstrap.min.css" rel="stylesheet" crossOrigin="anonymous"/>
            </Head>
            <Script src="https://cdn.jsdelivr.net/npm/bootstrap@4.6.0/dist/js/bootstrap.bundle.min.js" crossOrigin="anonymous" strategy="beforeInteractive"/>

            <div className="container">
                <nav className="navbar navbar-expand-lg navbar-dark bg-dark mt-3 rounded">
                    <a className="navbar-brand" href="#">
                        <Image src="/images/wultra-square.svg" width="30" height="30" alt="Wultra Logo"/>
                        <strong className="ml-2">Wultra Enrollment Server - iProov</strong>
                    </a>
                </nav>

                <div className="row mt-3">
                    <div className="col-sm-7">
                        <div className="card mb-3">
                            <h5 className="card-header">iProov demo</h5>
                            <div className="card-body">
                                <div className="card-text">
                                    <form>
                                        <label htmlFor="tokenValue">Token value </label>
                                        <input className="ml-1" id="tokenValue" name="tokenValue" type="text" value={token} onChange={handleChange} autoComplete="off" required />
                                        <button className="ml-1" type="submit">
                                            <Link href={{pathname: 'verify-token', query: {value: token}}} prefetch={false}>
                                                <a className="text-reset" target="_blank" rel="noreferrer">Verify</a>
                                            </Link>
                                        </button>
                                    </form>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div className="col-sm-5">
                        <div className="card mb-3">
                            <h5 className="card-header">Contact Us</h5>
                            <ul className="list-group list-group-flush">
                                <li className="list-group-item"><a target="_blank" rel="noreferrer" href="https://wultra.com">🔗 Website</a></li>
                                <li className="list-group-item"><a href="mailto:hello@wultra.com">✉️ E-mail</a></li>
                            </ul>
                        </div>
                    </div>
                </div>
            </div>
            <footer className="container mt-3">
                <p>&copy; <span>2021</span> <a target="_blank" rel="noreferrer" href="https://wultra.com">Wultra s.r.o.</a></p>
            </footer>
        </div>
    )
}
