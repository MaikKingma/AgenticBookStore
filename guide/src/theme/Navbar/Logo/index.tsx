import React, {type ReactNode} from 'react';
import Link from '@docusaurus/Link';
import useBaseUrl from '@docusaurus/useBaseUrl';
import ThemedImage from '@theme/ThemedImage';
import NavbarLogo from '@theme-original/Navbar/Logo';

// The OpenValue logo in front of the guide title (spec 14, decision D-S8). Docusaurus puts the
// theme logo and the title in ONE link, so the logo is its own link to openvalue.eu (new tab)
// and the original component keeps rendering only the title, which still goes to the guide
// home page. themeConfig.navbar.logo stays unset on purpose: the original would otherwise
// draw a second image inside the title link.
//
// The slate file is the light mode logo (the light navbar), the white file the dark mode logo
// (the slate navbar). The image paths go through useBaseUrl, so Pages (base URL) and the
// offline build (relative paths, hash router) both resolve them. The link carries the accessible
// name, the images are decorative.
export default function NavbarLogoWrapper(): ReactNode {
  const sources = {
    light: useBaseUrl('img/openvalue-logo-dark.png'),
    dark: useBaseUrl('img/openvalue-logo-light.png'),
  };
  return (
    <>
      <Link
        className="guide-navbar-logo"
        href="https://openvalue.eu"
        target="_blank"
        rel="noopener noreferrer"
        aria-label="OpenValue (opens in a new tab)">
        <ThemedImage sources={sources} alt="" height={32} />
      </Link>
      <NavbarLogo />
    </>
  );
}
