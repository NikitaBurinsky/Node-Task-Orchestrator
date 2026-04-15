import { expect, test } from '@playwright/test';
import { installMockApi } from './mockApi';

test.beforeEach(async ({ page }) => {
  await installMockApi(page);
});

test('command palette and global hotkeys navigate correctly', async ({ page }) => {
  await page.goto('/#/dashboard');
  await expect(page.getByRole('heading', { name: '$ dashboard' })).toBeVisible();

  await page.keyboard.press('ControlOrMeta+K');
  await expect(page.getByRole('dialog', { name: 'Command palette' })).toBeVisible();
  await page.getByPlaceholder('Type a command...').fill('Go to Tasks');
  await page.keyboard.press('Enter');

  await expect(page).toHaveURL(/#\/tasks/);

  await page.keyboard.press('g');
  await page.keyboard.press('d');
  await expect(page).toHaveURL(/#\/dashboard/);

  await page.keyboard.press('g');
  await page.keyboard.press('s');
  await expect(page).toHaveURL(/#\/servers/);

  const searchInput = page.getByPlaceholder('hostname, ip, user');
  await expect(searchInput).toBeVisible();
  await page.keyboard.press('/');
  await expect(searchInput).toBeFocused();
});

test('servers keep q-filter in URL and support bulk actions', async ({ page }) => {
  await page.goto('/#/servers');

  const searchInput = page.getByPlaceholder('hostname, ip, user');
  await searchInput.fill('web');
  await expect(page).toHaveURL(/#\/servers\?q=web/);

  await page.reload();
  await expect(searchInput).toHaveValue('web');
  await expect(page.getByText('web-01')).toBeVisible();

  await page.getByRole('button', { name: 'Select Visible' }).click();
  await expect(page.getByText('1 server selected')).toBeVisible();

  await page.getByRole('button', { name: 'Bulk Ping' }).click();
  await expect(page.getByText(/Bulk ping finished\./)).toBeVisible();

  await page.getByRole('button', { name: 'Bulk Delete' }).click();
  const confirmDialog = page.getByRole('dialog');
  await expect(confirmDialog).toBeVisible();
  await confirmDialog.getByRole('button', { name: 'Delete selected' }).click();

  await expect(page.getByText('web-01')).not.toBeVisible();
});

test('group ping filter persists in URL and task terminal shows status timeline', async ({ page }) => {
  await page.goto('/#/groups/1');

  await page.getByRole('button', { name: 'Ping All' }).click();
  await expect(page.getByText(/Ping completed\./)).toBeVisible();

  await page.getByRole('button', { name: /Offline/ }).click();
  await expect(page).toHaveURL(/pingFilter=offline/);

  await page.getByRole('button', { name: 'Execute Script' }).click();
  await page.getByText('deploy.sh').first().click();
  await page.getByRole('button', { name: 'Execute', exact: true }).click();

  await expect(page).toHaveURL(/#\/tasks\/\d+/);
  await expect(page.getByRole('heading', { name: 'Status Timeline' })).toBeVisible();
  await expect(page.getByText('RUNNING')).toBeVisible();
  await expect(page.getByText('SUCCESS', { exact: true })).toBeVisible({ timeout: 15000 });
});

test.describe('mobile navigation', () => {
  test.use({ viewport: { width: 390, height: 844 } });

  test('opens adaptive menu and navigates to tasks', async ({ page }) => {
    await page.goto('/#/dashboard');
    await page.getByRole('button', { name: 'Toggle navigation menu' }).click();
    await page.locator('nav').getByRole('link', { name: 'Tasks', exact: true }).last().click();
    await expect(page).toHaveURL(/#\/tasks/);
  });
});
