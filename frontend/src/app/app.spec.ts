import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { App } from './app';
import { AuthStore } from './core/auth-store';

/**
 * Tests du composant racine.
 * TestBed = mini application Angular factice (pas de vrai serveur).
 * provideHttpClientTesting() intercepte les appels HTTP pour qu'ils
 * n'aillent jamais sur le réseau.
 */
describe('App (coquille applicative)', () => {
  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([]), // routes vides : les liens ne naviguent pas vraiment
        provideHttpClient(),
        provideHttpClientTesting(), // intercepte toutes les requêtes HTTP
      ],
    }).compileComponents();
  });

  it('crée le composant', () => {
    const fixture = TestBed.createComponent(App);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('affiche les liens Connexion / Inscription quand déconnecté', async () => {
    TestBed.inject(AuthStore).logout();
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Connexion');
    expect(el.textContent).toContain('Inscription');
  });

  it('affiche le nom de l\u2019utilisateur et masque les liens de connexion quand connecté', async () => {
    TestBed.inject(AuthStore).setSession({
      token: 'jwt-de-test',
      type: 'Bearer',
      id: 1,
      email: 'jane@example.com',
      fullName: 'Jane Doe',
      roles: ['USER'],
    });
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Jane Doe');
    // Le bouton d'ouverture du menu utilisateur est présent (classe posée
    // par Material sur toute cible de menu) ; le contenu « Déconnexion »
    // ne se rend qu'à l'ouverture du menu.
    expect(el.querySelector('.mat-mdc-menu-trigger')).not.toBeNull();
    expect(el.textContent).not.toContain('S\u2019inscrire');
  });
});
