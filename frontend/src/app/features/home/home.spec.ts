import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { Home } from './home';

/**
 * TESTS UNITAIRES de la page d'accueil : l'état de santé de l'API
 * (point vert/rouge) doit refléter la réponse de /actuator/health.
 */
describe('Home (page)', () => {
  let controller: HttpTestingController;
  let fixture: ComponentFixture<Home>;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [Home],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    // Détruit la page pour résilier son intervalle RxJS avant le teardown.
    fixture?.destroy();
    controller.verify();
  });

  it('affiche UP (point vert) quand le backend répond', async () => {
    fixture = TestBed.createComponent(Home);
    await fixture.whenStable();

    // Le composant appelle /actuator/health au démarrage : on capture.
    const req = controller.expectOne('/actuator/health');
    expect(req.request.method).toBe('GET');
    req.flush({ status: 'UP' });
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('UP');
  });

  it('affiche DOWN (point rouge) quand le backend est injoignable', async () => {
    fixture = TestBed.createComponent(Home);
    await fixture.whenStable();

    controller.expectOne('/actuator/health').flush(null, { status: 0, statusText: 'Network Error' });
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('DOWN');
  });
});
